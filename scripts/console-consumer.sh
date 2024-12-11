#! /bin/zsh

docker exec -it kafka_broker kafka-console-consumer \
--bootstrap-server kafka.service:9092 \
--topic $1 \
--property print.key=true \
--property print.headers=true \
--offset earliest \
--partition 0
