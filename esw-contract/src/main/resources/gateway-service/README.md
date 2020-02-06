# Gateway service contract

This page describes possible ways of accessing gateway service via HTTP/Websocket and sample JSON representation of request body, response body and error body. 

## Gateway service endpoints 

### Http endpoint

The url - `http://{{host}}:{{port}}/post-endpoint` can be used to access gateway service

Following JSON payloads can be "post"ed to the above URL:
* ComponentCommand
* SequencerCommand
* PublishEvent
* GetEvent
* SetAlarmSeverity
* Log
* SetLogLevel
* GetLogMetadata

### Websocket endpoint

To execute the following request one can "send" it to the url - `ws://{{host}}:{{port}}/websocket-endpoint`.
* ComponentCommand
* SequencerCommand
* Subscribe
* SubscribeWithPattern

_Note:_

Make sure to replace the `host` and `port` of the running gateway service on local/remote machine.

## JSON Contract

* Request sample, response types and error types for http endpoint is described in `http-contract.json`     
* Request sample, response types and error types for webservice endpoint is described in `websocket-contract.json`
* Samples describing how to create requests, possible responses and possible errors can be found in `models.json`     

