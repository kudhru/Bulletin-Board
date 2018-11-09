1. Run `make all` to compile the project

2. To start the server on a machine run
    `java Server <ServerNum>`
    0 <= ServerNum < number of servers in IP.properties
    e.g. If the number of servers in IP.properties are 4, run the following commands for running all the four servers:
        `java Server 0`
        `java Server 1`
        `java Server 2`
        `java Server 3`
    
2. Start all the servers before running any tests.  

3. Run the following commands for running tests:
    1. `java TestReadYourWriteConsistency`
    2. `java TestSequentialConsistency`
    3. `java TestQuorumConsistency`
    4. `java ClientTestDriver`
    
4. Interactive Client shell: The below command allows you to interactively perform READ/WRITE operations on the servers.
    `java ClientTerminal`
