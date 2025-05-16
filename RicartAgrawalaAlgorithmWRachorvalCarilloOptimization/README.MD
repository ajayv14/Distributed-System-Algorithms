The Ricart-Agrawala algorithm is a distributed mutual exclusion algorithm that optimizes Lamport's Distributed Mutual Exclusion Algorithm by eliminating the need for release messages. Here's a summary of the algorithm and the Roucairol-Carvalho optimization:

Ricart-Agrawala Algorithm:

1. Requesting the Critical Section:

When a site wants to enter the critical section, it sends a timestamped REQUEST message to all other sites. The timestamp is based on the site's logical clock.

2. Receiving a Request:

Upon receiving a REQUEST message, a site sends a timestamped REPLY message if and only if:
It is not currently interested in the critical section.
It is requesting the critical section but has a lower priority (a later timestamp) than the incoming request.
Otherwise, the receiving site defers its reply.

3. Entering the Critical Section:

The requesting site can enter the critical section only after it has received REPLY messages from all other sites.

4. Releasing the Critical Section:

Upon exiting the critical section, the site sends all its deferred REPLY messages.
Performance:

Requires 2(N−1) messages per critical section entry, where N is the number of sites.
Synchronization delay is one message propagation delay.


Roucairol-Carvalho Optimization:

This optimization aims to reduce the number of messages required for subsequent critical section entries by the same site.

Once a site Si  has received a REPLY from another site Sj granting permission to enter the critical section, 
Si  can enter the critical section multiple times without needing to send another REQUEST to Sj,
until Sj  itself sends a REQUEST to enter its critical section.


In essence, the Roucairol-Carvalho optimization allows a site to reuse the "permission" it received from other sites for subsequent entries, as long as those other sites haven't made their own requests in the meantime. This reduces the message overhead in scenarios where a site needs to enter the critical section repeatedly.




Steps to Run : 

javac *.java


Run the commands below each in a different terminal window :

java Sockerver node01

java Sockerver node02 

java Sockerver node03 

java Sockerver node04 