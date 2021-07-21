#!/usr/bin/env bash

# Check if two input arguments provided
if [ "$#" -ne 2 ]
then
    echo "Please provide the following arguments: <gateway_ip> <access_token>"
    echo "e.g. ./publish.sh 10.1.1.1 eyJhbGciOiJSUzI1NiIsInR5cCIgO..."
    exit 1
fi

ip=$1
token=$2

for i in {1..10000}
do
    curl -X POST \
    http://$ip:8090/post-endpoint \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    -H "X-Real-IP: $ip" \
    -H "Username: osw-user1" \
    -H "App-Name: eng-ui" \
    -d '{
        "_type": "PublishEvent",
        "event": {
            "_type": "ObserveEvent",
            "eventId": "1a745d6b-30b6-4503-a09e-e3bdbb680795",
            "source": "CSW.ncc.trombone",
            "eventName": "offline4",
            "eventTime": "2020-02-18T05:57:09.754352Z",
            "paramSet": []
        }
    }'
    
    sleep 0.2
done
