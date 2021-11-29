import os
import csv
import random

queries = []

for file in os.listdir(os.getcwd()):
    with open(os.path.join(os.getcwd(), file), 'r') as f:
        file = csv.reader(f)
        queries.append(line[1].replace(' ','+') + '\n' for line in file)
        print(queries)
        exit()

random.shuffle(queries)

num_partitions = 20 # number of files to split queries into

partition_size = len(queries) / num_partitions

for i in range(20):
    with open(os.path.join(os.getcwd(), f'queries_{i}.txt'), 'w') as f:
        f.writelines(queries[i * partition_size: (i + 1) * partition_size])
