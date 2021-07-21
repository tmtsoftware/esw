#!/usr/bin/env bash

# Check if two input arguments provided
if [ "$#" -ne 1 ]
then
    echo "Please provide the following arguments: <gateway_ip>"
    echo "e.g. ./subscribeEventPattern.sh 10.1.1.1"
    exit 1
fi

ip=$1

wscat  -x '{ "_type" : "SubscribeWithPattern", "subsystem" : "CSW", "maxFrequency" : 10, "pattern" : "[a-z]*" }' -H "X-Real-IP:$ip" -w 100000 -c "ws://$ip:8090/websocket-endpoint?App-Name=event-dashboard&Username=event-admin"