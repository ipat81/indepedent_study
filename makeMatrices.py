import csv
import json
import matplotlib
from matplotlib import path
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from collections import deque
from itertools import repeat
import threading
import Queue
import math
import time
import sys


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


def add_to_paths(work, polygon_list, vehicle_paths, region_points):
   #  print threading.currentThread()
    which_polygon_time = 0
    append_to_list_time = 0
    create_object_time = 0
    while True:
        path = work.get()
        for row in path:
            # v = VehicleLocation.VehicleLocation(row[0], row[1], row[2], row[3], polygon_list)
            start = time.time()
            region = -1
                
            # get 10 closest regions
            distance_list = make_distance_list(float(row[2]), float(row[3]), region_points)
            smallest_indices = [-1] * 10
            for i in xrange(10):
                smallest = sys.float_info.max
                for j in xrange(len(distance_list)):
                    distance = distance_list[j]
                    if distance < smallest:
                        smallest = distance
                        smallest_indices[i] = j
                distance_list[smallest_indices[i]] = sys.float_info.max

            # check if in top 10
            for index in smallest_indices:
                if polygon_list[index].contains_points([(float(row[2]), float(row[3]))]):
                    region = index
                    break
            
            if region == -1:
                region = which_polygon(float(row[2]), float(row[3]), polygon_list)
            # region = which_polygon_pnpoly(float(row[2]), float(row[3]), polygon_list)
            end = time.time()
            which_polygon_time += (end - start)

            start = time.time()
            v = [int(row[0]), int(row[1]), float(row[2]), float(row[3]), region]
            end = time.time()
            create_object_time += (end-start)

            start = time.time()
            # vehicle_paths[v.id].append(v)
            vehicle_paths[v[0]].append(v)
            end = time.time()
            append_to_list_time += (end-start)


        work.task_done()
        print 'after work_done', threading.currentThread(), 'which_polygon_time: ', which_polygon_time, 'append time: ', \
            append_to_list_time, 'create time: ', create_object_time, 'region: ', region


def make_matrices(vehicle_list):

    matrices = [[]]

    for vl in vehicle_list:
        for v in vl:
            print 'a'


def make_vehicle_array(filename, polygon_list, region_points):
    # GENERATES GRAPH OF REGIONS
    # fig = plt.figure()
    # ax = fig.add_subplot(111)
    # for polygon in polygon_list:
    #     patch = patches.PathPatch(polygon, facecolor='orange', lw=2)
    #     ax.add_patch(patch)
    # ax.set_xlim(113, 115)
    # ax.set_ylim(21, 23)
    f = open(filename, 'rb')
    reader = csv.reader(f)

    last_vehicle_id = int(get_last_row(reader)[0])

    vehicle_paths = [[] for i in repeat(None, last_vehicle_id+1)]
    # vehicle_paths = np.array([[], []])
    work = Queue.Queue()
    for i in xrange(1):  # (last_vehicle_id):
        t = threading.Thread(target=add_to_paths, args=(work, polygon_list, vehicle_paths, region_points))
        t.daemon = True
        t.start()

    f.seek(0)
    temp = []
    curV = -1
    for row in reader:
        if len(row) != 4:  # each row should be id, time, long, lat
            continue

        if curV == -1:
            curV = int(row[0])

        if curV == int(row[0]):
            temp.append(row)
        else:
            work.put(temp)
            temp = []
            temp.append(row)
            curV = int(row[0])

        #v = VehicleLocation.VehicleLocation(row[0], row[1], row[2], row[3], polygon_list)
        #vehicle_paths[v.id].append(v)
        #print v.id
    work.put(temp)

    work.join()

    return vehicle_paths


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


start = time.time()
plist_path = make_polygon_list()
plist = make_pnpoly_polygon_list()
region_points = make_region_points(plist)



vehicle_paths = make_vehicle_array("small_private_raw_p.txt", plist_path, region_points)
print 'TOTAL TIME: ', time.time()-start
# a = np.zeros([],[])


# c = make_region_points(plist)
#
# for p in c:
#     print p

# for v in vehicle_paths[4]:
#     print v



