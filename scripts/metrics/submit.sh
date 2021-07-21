#!/usr/bin/env bash

# Check if two input arguments provided
if [ "$#" -ne 2 ]
then
    echo "Please provide the following arguments: <gateway_ip> <access_token>"
    echo "e.g. ./submit.sh 10.1.1.1 eyJhbGciOiJSUzI1NiIsInR5cCIgO..."
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
      "_type": "ComponentCommand",
      "componentId": {
        "prefix": "CSW.ncc.trombone",
        "componentType": "hcd"
      },
      "command": {
        "_type": "Submit",
        "controlCommand": {
          "_type": "Observe",
          "source": "CSW.ncc.trombone",
          "commandName": "immediate",
          "maybeObsId": "2020A-001-123",
          "paramSet": []
        }
      }
    }'
    
    sleep 1
done
