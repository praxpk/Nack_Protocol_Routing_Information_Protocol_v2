# NACK Protocol RIP v2 node communicator
## This project creates a node in docker that communicates with other instances using RIP protocol.

Communication occurs through binary, each node serializes and deserializes packet information. Each packet adheres to the RIP V2 protocol.
Each node maintains a table of neighbours, the shortest steps it takes to reach them and the next node along that path.

Run each Rover.java program on a separate machine/vm/docker instance.
Assign a fictitious ip address to each rover. If you wish to transfer
a file to another instance, mention its fictitious ip address and
the file you wish to transfer in the arguments.

java Rover fictitious_ip_address [destination_ip file_name] 

To test this in Docker check out:
https://github.com/ProfFryer/MulticastTestingEnvironment
