#!/usr/bin/env bash

for i in {1..10000}
do
curl -X POST \
  http://localhost:8090/post-endpoint \
  -H 'Content-Type: application/json' \
  -H 'Postman-Token: 922383f7-8934-437d-abe0-67a72295c5d2' \
  -H 'cache-control: no-cache' \
  -H 'X-Real-IP: 10.131.20.155' \
  -d '{
  "_type": "PublishEvent",
  "event": {
    "_type": "ObserveEvent",
    "eventId": "1a745d6b-30b6-4503-a09e-e3bdbb680795",
    "source": "CSW.filter.wheel2",
    "eventName": "offline",
    "eventTime": "2020-02-18T05:57:09.754352Z",
    "paramSet": []
  }
}'

sleep 0.5
done
