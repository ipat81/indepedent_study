import csv


def create_smaller_file(filename):
    original_file = open(filename)
    reader = csv.reader(original_file)

    small_filename = 'small_' + filename
    new_file = open(small_filename, 'w')

    previous_id = -1

    for row in reader:
        if len(row) > 1:
            curr_id = int(row[0])

        if curr_id != previous_id:
            if curr_id == 100:
                break

        for entry in row:
            if row[len(row)-1] == entry:
                new_file.write(str(entry))
            else:
                new_file.write(str(entry) + ',')
        new_file.write('\n')


create_smaller_file('private_raw_p.txt')
