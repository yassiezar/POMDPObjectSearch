#!/usr/bin/python

import socket, datetime, atexit

def get_filename():
    return datetime.datetime.now().strftime("%Y-%m-%d_%I:%M:%S")	

host = "10.5.42.163"
port = 6666

print (host)
print (port)

serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
serversocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
serversocket.bind((host, port))

f = open(get_filename() + '.csv', 'w')


def close_socket():
    print 'Closing Socket...'
    serversocket.close()
    print 'Closing File...'
    f.close()

atexit.register(close_socket)
serversocket.listen(5)
print ('server started and listening')
while 1:
    clientsocket, address = serversocket.accept()
    data = clientsocket.recv(1024)
    f.write(data + '\n')
    print data
