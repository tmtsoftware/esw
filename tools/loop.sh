#!/bin/bash
for i in {1..1000}
do
curl -X POST \
  http://localhost:8090/post-endpoint \
  -H 'Content-Type: application/json' \
  -H 'Postman-Token: 922383f7-8934-437d-abe0-67a72295c5d2' \
  -H 'cache-control: no-cache' \
  -d '{
  "GetEvent": {
    "eventKeys": [
      {
        "source": "wfos.blue.filter",
        "eventName": "filter_wheel"
      }
    ]
  }
}'
done