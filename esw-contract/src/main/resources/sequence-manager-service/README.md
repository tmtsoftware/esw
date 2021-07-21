# Sequence Manager service contract

This page describes possible ways of accessing sequence manager service via HTTP and sample JSON representation of request body, response body and error body.

## Sequence Manager service endpoints

### Http endpoint

The url - `http://{{host}}:{{port}}/post-endpoint` can be used to access Sequence Manager service

Following JSON payloads can be "post"ed to the above URL:

* GetObsModesDetails
* GetAgentStatus
* GetResources
* Configure
* Provision
* StartSequencer
* RestartSequencer
* ShutdownSequencer
* ShutdownSubsystemSequencers
* ShutdownObsModeSequencers
* ShutdownAllSequencers
* ShutdownSequenceComponent
* ShutdownAllSequenceComponents

_Note:_

Make sure to replace the `host` and `port` of the running sequencer service on local/remote machine.

## JSON Contract

* Request sample, response types and error types for http endpoint is described in `http-contract.json`
* Samples describing how to create requests, possible responses and possible errors can be found in `models.json`
