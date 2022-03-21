import sys
import os

root_path = sys.argv[1]
out_file_name = 'file_list.txt'

if root_path is None:
	raise Exception("Specify root path!")

dir_list = [f for f in os.listdir(root_path) if os.path.isdir(os.path.join(root_path, f))]

print(dir_list)

with open(out_file_name, "w+") as f:
	for d in dir_list:
		f.write(d)
		f.write("\n")
