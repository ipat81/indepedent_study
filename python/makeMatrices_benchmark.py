"""
- all times are in seconds
"""
import csv, json, math, time, sys
from collections import deque
from itertools import repeat

import matplotlib
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib import path

import pandas as pd

MIN_STOP_TIME = 180
MATRIX_TIME_INTERVAL = 300


def make_polygon_list():
    polygon_list = []
    with open('shenzhen_tran_mapbox_polygon.json') as data_file:
        data = json.load(data_file)
        for x in range(0, len(data['features'])):
            p = path.Path(data['features'][x]['geometry']['coordinates'][0])
            polygon_list.append(p)

    return polygon_list


def make_pnpoly_polygon_list():
    polygon_list = []
    with open('shenzhen_tran_mapbox_polygon.json') as data_file:
        data = json.load(data_file)
        for x in range(0, len(data['features'])):
            p = data['features'][x]['geometry']['coordinates'][0]
            polygon_list.append(p)

    return polygon_list


def get_last_row(reader):
        try:
            lastrow = deque(reader)[-3]
        except IndexError:  # empty file
            lastrow = None
        return lastrow


def which_polygon(lon, lat, polygon_list):
        x = 0
        for polygon in polygon_list:
            if polygon.contains_points([(lon, lat)]):
                return x
            x += 1
        return x


def which_polygon_pnpoly(lon, lat, polygon_list):
    x = 0
    p = (lon, lat)
    for polygon in polygon_list:
        if pnpoly.cnpnpoly(p, polygon):
            return x
        x += 1
    return x


def make_region_points(polygon_list):
    centroids = []
    for p in polygon_list:
        x = 0
        y = 0
        for coords in p:
            x += coords[0]
            y += coords[1]
        centroids.append([x/len(p), y/len(p)])
    return centroids


def distance(p0, p1):
    return math.sqrt((p0[0] - p1[0])**2 + (p0[1] - p1[1])**2)


def make_distance_list(lon, lat, region_points):
    distances = []
    for r in region_points:
        distances.append(distance((lon,lat), (r[0],r[1])))
    return distances


def make_matrices(vehicle_paths, num_regions):
    num_matrices = 86400 / MATRIX_TIME_INTERVAL
    matrices = [[[0] * num_regions for j in xrange(num_regions)] for i in xrange(num_matrices)]

    for path in vehicle_paths:
        # ya never know
        if len(path) < 2:
            continue

        origin_region = path[0][4]
        origin_time = path[0][1]
        origin_coord = (path[0][2], path[0][3])
        last_location_coord = last_location_time = last_location_region = None
        last_location_duration = 0

        for i in xrange(1, len(path)):
            location = path[i]
            location_time = location[1]
            location_coord = (location[2], location[3])
            location_region = location[4]

            # we are still at the previous destination stop so skip
            if location_coord == origin_coord:
                continue
            
            # just set a new origin so theres no last location since leaving the origin
            if not last_location_coord:
                last_location_coord = location_coord
                last_location_time = location_time
                last_location_region = location_region
            # the vehicle stayed in the same place
            elif last_location_coord == location_coord:
                # update time spent at location
                last_location_duration += location_time - last_location_time
                last_location_time = location_time

                if last_location_duration >= MIN_STOP_TIME:
                    # update matrix
                    matrix_index = (origin_time % 86400) / 300
                    matrices[matrix_index][origin_region][last_location_region] += 1

                    # update origin to last location and reset last location
                    origin_region = last_location_region
                    origin_time = last_location_time
                    origin_coord = location_coord
                    last_location_coord = last_location_time = last_location_region = None
                    last_location_duration = 0
            # the vehicle moved to another location
            else:
                last_location_coord = location_coord
                last_location_time = location_time
                last_location_region = location_region
                last_location_duration = 0
    
    # TODO: write to .mat file
    return matrices


def make_vehicle_array(filename, polygon_list, region_points, use_pandas=False):
    total_time = time.time()
    f = open(filename, 'rb')
    rows = None
    last_vehicle_id = -1

    if use_pandas:
        print 'USING PANDAS'
        rows = pd.read_csv(filename).itertuples()
        last_vehicle_id = int(get_last_row(rows)[1])
        rows = pd.read_csv(filename).itertuples()
    else:
        print 'USING STDLIB'
        rows = csv.reader(f)
        last_vehicle_id = int(get_last_row(rows)[0])
        f.seek(0)

    vehicle_paths = [[] for i in repeat(None, last_vehicle_id+1)]

    total_top10_time = 0
    total_whichpolygon_time = 0
    total_create_time = 0
    total_append_time = 0

    for row in rows:
        if len(row) < 4 or any(pd.isnull(val) for val in row):  # each row should be id, time, long, lat
            continue

        offset = 1 if use_pandas else 0

        region = -1

        top10_time = time.time()
        # 10 closest regions
        distance_list = make_distance_list(float(row[2 + offset]), float(row[3 + offset]), region_points)
        smallest_indices = [-1] * 10
        for i in xrange(10):
            smallest = sys.float_info.max
            for j in xrange(len(distance_list)):
                distance = distance_list[j]
                if distance < smallest:
                    smallest = distance
                    smallest_indices[i] = j
            distance_list[smallest_indices[i]] = sys.float_info.max
        total_top10_time += time.time() - top10_time

        # check top 10
        for index in smallest_indices:
            if polygon_list[index].contains_points([(float(row[2 + offset]), float(row[3 + offset]))]):
                region = index
                break

        which_polygon_time = time.time()
        # check the rest
        if region == -1:
            region = which_polygon(float(row[2 + offset]), float(row[3 + offset]), polygon_list)
        total_whichpolygon_time += time.time() - which_polygon_time

        create_time = time.time()
        v = [int(row[0 + offset]), int(row[1 + offset]), float(row[2 + offset]), float(row[3 + offset]), region]
        total_create_time += time.time() - create_time

        append_time = time.time()
        vehicle_paths[v[0]].append(v)
        total_append_time += time.time() - append_time

    f.close()

    total_time = time.time() - total_time
    print 'TOTAL TOP10 TIME: ', total_top10_time, '\n', \
        'TOTAL WHICHPOLYGON TIME: ', total_whichpolygon_time, '\n', \
        'TOTAL CREATE TIME: ', total_create_time, '\n', \
        'TOTAL APPEND TIME: ', total_append_time, '\n', \
        'TOTAL TIME: ', total_time

    return vehicle_paths


if __name__ == '__main__':
    plist_path = make_polygon_list()
    plist = make_pnpoly_polygon_list()
    region_points = make_region_points(plist)
    vehicle_paths = make_vehicle_array("small_private_raw_p.txt", plist_path, region_points)
    # we add 1 to arg2 for the "out of city" region
    #matrices = make_matrices(vehicle_paths, len(plist) + 1)