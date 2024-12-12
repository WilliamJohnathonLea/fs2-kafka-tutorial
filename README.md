# FS2 Kafka Tutorial

## Feed Steps
### Retrieve Feed State
Make a call to Redis to check if the feed already exists.
If it does, then request the data. If not, then inject a new state.

A feed should only not have a state in Redis before the first packet is received. If a packet with a sequence number greater than 1 is received and the state doesn't exist in Redis then log a warning.

### Enrich the feed
Make an HTTP call to an external service which will provide the names of the players and teams.

### Strip duplicates
If a packet has a sequence number less than or equal the sequence number in the state it is a duplicate and is stripped from the feed.

### Transform the inbound payload to an outbound format
Data suppliers send data in various formats so the transformation stage is used
to convert those formats into a standard format.

### Save the state
After all the processing, the new state should be saved to Redis for the next packet in the feed.

### Save the packet
After the state is saved we can save the transformed packet to kafka.
