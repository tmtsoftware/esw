# Agent service contract

This page describes possible ways of accessing agent service via HTTP/Websocket and sample JSON representation of request body, response body and error body.

## Agent service endpoints

### Http endpoint

The url - `http://{{host}}:{{port}}/post-endpoint` can be used to access sequencer service

Following JSON payloads can be "post"ed to the above URL:
* spawnSequenceManager
* spawnSequenceComponent
* killComponent

_Note:_

Make sure to replace the `host` and `port` of the running sequencer service on local/remote machine.

## JSON Contract

* Request sample, response types and error types for http endpoint is described in `http-contract.json`
* Request sample, response types and error types for webservice endpoint is described in `websocket-contract.json`
* Samples describing how to create requests, possible responses and possible errors can be found in `models.json`

