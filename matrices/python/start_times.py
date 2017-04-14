import sys, csv
from collections import Counter

TIME_DELTA = 60 * 10

def print_start_times(filename):
    f = open(filename, 'rb')
    reader = csv.reader(f)

    start_times_count = Counter()
    prev_vid = -1
    for row in reader:
        if len(row) < 4:
            continue

        vid = int(row[0])
        if prev_vid != vid:
            # new start time
            start_time = int(row[1]) / TIME_DELTA
            start_times_count[start_time] += 1

            prev_vid = vid

    print '10 most common time intervals: ', start_times_count.most_common(10)

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print 'Usage: python start_times.py <data_file>'
        exit()

    print_start_times(sys.argv[1])
