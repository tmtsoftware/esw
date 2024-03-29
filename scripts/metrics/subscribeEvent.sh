#!/usr/bin/env bash

# Check if two input arguments provided
if [ "$#" -ne 1 ]; then
    echo "Please provide the following arguments: <gateway_ip>"
    echo "e.g. ./subscribeEvent.sh 10.1.1.1"
    exit 1
fi

ip=$1

wscat -x '{ "_type" : "Subscribe", "eventKeys" : [ { "source" : "CSW.ncc.trombone", "eventName" : "offline4" } ], "maxFrequency" : 10 }' -w 100000 -c "ws://$ip:8090/websocket-endpoint?appName=event-dashboard&username=Bob"
