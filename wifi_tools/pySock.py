#!/usr/bin/python3

import socket, datetime, atexit, argparse

def get_filename(obj=None):
    fstring = datetime.datetime.now().strftime("%Y-%m-%d_%I:%M:%S")	
    if obj is not None:
        fstring = obj + '_' + fstring 
    return fstring

host = "10.42.0.1"
port = 6666

print(host)
print(port)

parser = argparse.ArgumentParser(description='Record experiment data from wifi socket')
parser.add_argument('object', metavar='o', type=str, nargs='?', help='the object being searched for')

args = parser.parse_args()
obj = args.object

serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
serversocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
serversocket.bind((host, port))

f = open(get_filename(obj) + '.csv', 'w')

def close_socket():
    print('Closing Socket...')
    serversocket.close()
    print('Closing File...')
    f.close()

atexit.register(close_socket)
serversocket.listen(5)
print('Server started and listening')
while 1:
    clientsocket, address = serversocket.accept()
    data = clientsocket.recv(1024)
    print(data.decode().strip('\0'))
    f.write(data.decode().strip('\0'))
    f.write('\n')
