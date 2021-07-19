#!/usr/bin/env bash

# Check if one input arguments provided
if [ "$#" -ne 1 ]
then
    echo "Please provide the following arguments: <gateway_ip>"
    echo "e.g. ./getEvent.sh 10.1.1.1"
    exit 1
fi

ip=$1

for i in {1..10000}
do
    curl -X POST \
    http://$ip:8090/post-endpoint \
    -H "Content-Type: application/json" \
    -H "X-Real-IP: $ip" \
    -d '{
      "_type": "GetEvent",
      "eventKeys": [
        {
          "source": "CSW.ncc.trombone",
          "eventName": "offline"
        }
      ]
    }'
    
    sleep 1
done

