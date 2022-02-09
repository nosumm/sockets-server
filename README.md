# sockets-server

## Project 1:

### Part 1:

In part 1 I created a client application that communicates with a server using a specific protocol. The client's task is to follow the protocol as closely as possible and to extract a secret from the server for each stage of the protocol. The server's task is to validate that the client is following the protocol -- any deviation of the client from the protocol will cause the server to close the connection. The client and the server will communicate over UDP as well as TCP sockets.

### Part 2:

In part 2 I write a web server. The server's task is to verify whether the client adheres to the protocol and send a response only to the valid client. The server can handle multiple clients at a time. 

### Client-Server Communication Protocol:

The server will run on the host on attu2.cs.washington.edu and attu3.cs.washington.edu, listening for incoming packets on UDP port 12235. The server expects to receive and will only send:

    payload that has a header (see below)
    data in network-byte order (big-endian order)
    4-byte integers that are unsigned (uint32_t), or 2-byte integers that are unsigned (uint16_t)
    characters that are 1-byte long (Note: in Java a char is 2-byte long)
    strings that are a sequence of characters ending with the character '\0'
    packets that are aligned on a 4-byte boundary (that is, a packets must be padded until its length is divisible by 4)

The server will close any open sockets to the client and/or fail to respond to the client if:

    unexpected number of buffers have been received
    unexpected payload, or length of packet or length of packet payload has been received
    the server does not receive any packets from the client for 3 seconds

Every payload (TCP and UDP) sent to the server and sent by the server must have a packet header. This header must be located in the leading bytes of the transmission, prefixed to the payload. The header has a constant length of 12-bytes. The first four bytes of the header contain the payload length of the packet (excluding any padding to byte-align the packet). The next four bytes contain the secret of the previous stage of the protocol, psecret. The next two bytes contain an integer step number of the current protocol stage. For example, for step c1, the header's first four bytes will contain the length of the packet, the next four bytes will contain secretB, and the following two bytes will be set to the value 1. Note: for Client side, the step number will always be 1 since you are doing step 1 at each stage while the server is doing step 2. For stage a, psecret is defined as 0. The last two bytes of the header should be set to an integer representation of the last 3 digits of (one of) your student number. This 12-byte header does not count towards the length of the payload (which is to be 4-byte aligned). Throughout this part 1 description we will use diagrams such as the following to describe packet formats; here is the format of the packet header for part 1:


 0               1               2               3
 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                          payload_len                          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                            psecret                            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|              step             |   last 3 digits of student #  |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

The numbers at the top indicate the bit number in rows of 32 bits, and fields are separated by +- and | marks. The header is omitted from the following packet diagrams to eliminate redundancy, but remember that every packet has to have the header above.

STAGE a:
Step a1. The client sends a single UDP packet containing the string "hello world" without the quotation marks to attu2.cs.washington.edu (referred to as the 'server') on port 12235:


 0               1               2               3
 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                                                               |
|                          hello world                          |
|                                                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

Note: 'hello world' is not actually 4 bytes.

Step a2. The server responds with a UDP packet containing four integers: num, len, udp_port, secretA:


 0               1               2               3
 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                              num                              |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                              len                              |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                            udp_port                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                            secretA                            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+


STAGE b:
Step b1. The client reliably transmits num UDP packets to the server on port udp_port. Each of these 'data' packets has length len+4 (remember that each packet's entire payload must be byte-aligned to a 4-byte boundary). The first 4-bytes of each data packet payload must be integer identifying the packet. The first packet should have this identifier set to 0, while the last packet should have its counter set to num-1. The rest of the payload bytes in the packet (len of them) must be 0s. The packet header length does not count towards len:


 0               1               2               3
 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                           packet_id                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                                                               |
|                     payload of length len                     |
|                                                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

For each received data packet, the server will acknowledge (ack) that packet by replying with an 'ack' packet that contains as the payload the identifier of the acknowledged packet:


 0               1               2               3
 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        acked_packet_id                        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

 

To complete this step, the client must receive ack packets from the server for all num packets that it generates. To do so, the client resends those packets that the server does not acknowledge. The client should use a retransmission interval of at least .5 seconds.

Step b2. Once the server receives all num packets, it sends a UDP packet containing two integers: a TCP port number, secretB.


 0               1               2               3
 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                            tcp_port                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                            secretB                            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+


STAGE c:
Step c1. The client opens a TCP connection to the server on port tcp_port received from the server in step b2.

Step c2. The server sends three integers: num2, len2, secretC, and a character: c.


 0               1               2               3
 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                              num2                             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                              len2                             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                            secretC                            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|       c       |
+-+-+-+-+-+-+-+-+

 

Note: If you receive 16 bytes as the payload_len, it's a mistake in our implementation so you can disregard that as it doesn't affect any of the later stages anyway. However don't make the same mistake in your part 2 stage c2.

STAGE d:
Step d1. The clients sends num2 payloads, each payload of length len2, and each payload containing all bytes set to the character c.


 0               1               2               3
 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                                                               |
|           payload of length len2 filled with char c           |
|                                                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

 

Step d2. The server responds with one integer payload: secretD:


 0               1               2               3
 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                            secretD                            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+




## Project 2:

### Objective: 

Learn about Software Defined Networking (SDN). 
Use Virtualbox, Mininet, and Pox as the implementers of the OpenFlow protocol, you will build simple networks using SDN primitives. 

## Project 3:

### Objective:

Use Mininet to study the bufferbloat phenomenon. Compare the performance of TCP Reno and TCP BBR over a network with slow uplink connection. 
