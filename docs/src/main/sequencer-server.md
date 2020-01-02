# sequencer-http-server

When sequencer is started on machine, it also starts embedded http server on random free port. This embedded sequencer
server is registered with location service so that it can be discovered and used to communicate with sequencer component.
sequencer-http-server exposes http endpoints of `SequencerCommandService` which enables to submit sequence to sequencer and query
for response. It also exposes other endpoints (`SequencerApi`) which allows to edit sequence, pause/resume sequence, 
sequencer lifecycle enpdoints etc. sequencer-http-server http interface is explained in detail in Routes section.

## Prerequisite

It requires csw-location-server running on machine. The csw-services.sh script does this for you. 
This application will start a HTTP CSW Location Server on port 7654 which is required for all Location Service consumers who uses HTTP Location client. 
All components (HCD's, Assemblies, Services, Sequencer, sequencer-http-server etc.) use a local HTTP Location client which 
expects the Location Server running at localhost:7654. In a production environment, it is required that all machines running 
components should have the HTTP Location Server running locally.

## Starting sequencer-http-server

When sequencer app is started, it starts sequencer server and registers it with location server. Sequencer is registered as akka as well as
http location. For example, sequencer with subsystem `iris` and observing mode `darknight` is registered with `iris.darknight` (<subsystem>.<observing>) prefix.

## http endpoints

@@@note

Please replace port in following examples with appropriate sequencer-server port. e.g. - `POST http://localhost:<your-port>/post-endpoint`
Please use appropriate id in respective APIs. e.g. - delete, addBreakpoint etc.

@@@

### Sequencer command service
This allows to submit sequence to sequencer and query for response

#### Submit

This endpoint allows to submit sequence to sequencer. Sequencer will start executing sequence. This api gives `SubmitResponse` future as response. 
Submit won't wait for final response. It will give started response saying sequence is submitted successfully and started.

Submit
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #submit }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #submit-response }

##### Re-Submit sequence when previous sequence is in progress

Re-Submit
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #re-submit }

200 OK with Domain error
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #re-submit-response }

#### Malformed Request

Malformed request for submit
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #malformed-submit }

400 Bad Request
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #malformed-submit-response }

#### Query

This endpoint allows to query for sequence response. Query response can be intermediate reponse or final response. 

Query
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #query }

200 OK [Started Response]
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #query-response-started }

200 OK [Completed Response]
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #query-response-completed }

#### QueryFinal

This endpoint allows to query for final response of sequence.

QueryFinal (Websocket) 

```.http request
ws://localhost:57236/websocket-endpoint

{
  "QueryFinal": "6ed2bf72-d7a3-498f-85e4-b3719727d18c"
}

```

Responses

Started :

````json
{
  "Started": {
    "runId": "6ed2bf72-d7a3-498f-85e4-b3719727d18c"
  }
}
````

Completed:

````json
{
  "Completed": {
    "runId": "6ed2bf72-d7a3-498f-85e4-b3719727d18c"
  }
}
````

@@@ note

One can use websocket plugin for [IntelliJ](https://plugins.jetbrains.com/plugin/7980-websocket-client/) or [chrome extension]
(https://chrome.google.com/webstore/detail/simple-websocket-client/pfdhoblngboilpfeibdedpjgfnlcodoo?hl=en) for QueryFinal functionality

@@@


### Sequencer editor APIs

This exposes APIs to edit sequence, get status of sequence

#### loadSequence

This api loads sequence in sequencer. Sequencer will be in `Loaded` state. Sequencer will wait for StartSequence command to
start execution.

LoadSequence
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #loadSequence }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #loadSequence-response }

#### startSequence

This api starts loaded sequence in sequencer. This gives `Started` as response. Api returns `Unhandled Response` is sequence
is not loaded. 

StartSequence
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #startSequence }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #startSequence-response }

#### getSequence

This api allows to get state of sequence submitted.

GetSequence
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #getSequence }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #getSequence-response }

#### add

This api allows to add more commands to running sequence. These commands will be added at the end of sequence.

Add
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #add }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #add-response }

#### prepend

This api allows to add more commands to running sequence. These commands will be prepended to all pending commands of sequence.

Prepend
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #prepend }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #prepend-response }

#### replace

This api allows to replace command with given id with list of commands. This api will return error response if provided id
does not exist or command to be replaces is finished or in progress.

Replace
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #replace }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #replace-response }

#### insertAfter

This api allows to add more commands to sequence after command with provided run id. This api return error is provided id does not exist
or sequence execution has gone ahead of provided id.

InsertAfter
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #insertAfter }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #insertAfter-response }

#### delete

This api allows to delete command with provided id.

Delete
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #delete }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #delete-response }

#### addBreakpoint

This api allows to put breakpoint at command with provided id. Sequence execution will pause where breakpoint is applied.

AddBreakpoint
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #addBreakpoint }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #addBreakpoint-response }

#### removeBreakpoint

This api allows to remove breakpoint from command with provided id.

RemoveBreakpoint
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #removeBreakpoint }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #removeBreakpoint-response }

#### reset

This api allows to remove all pending commands from sequence.

Reset
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #reset }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #reset-response }

#### pause

This api allows to pause sequence. Command which is currently in progress will execute but sequence execution will not proceed.

Pause
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #pause }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #pause-response }

#### resume

This api allows to resume sequence from paused point.

Resume
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #resume }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #resume-response }

#### getSequenceComponent

This api allows to get location of sequence component where sequencer is running.

GetSequenceComponent
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #getSequenceComponent }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #getSequenceComponent-response }

#### isAvailable

This api allows to check whether sequencer is executing any sequence. It return true if any sequence is in execution else false.

IsAvailable
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #isAvailable }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #isAvailable-response }

### Sequencer lifecycle APIs

This exposes endpoints for sequencer lifecycle APIs e.g - goOffline, diagnostic mode, abortSequence etc.

#### isOnline

This api allows to check whether sequencer is online.

IsOnline
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #isOnline }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #isOnline-response }

#### abortSequence

This api allows to abort sequence by removing all pending commands and also executes abort handler written in sequencer script.

AbortSequence
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #abortSequence }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #abortSequence-response }

#### stop

This api allows to stop sequence by removing all pending commands and also executes stop handler written in sequencer script.

Stop
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #stop }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #stop-response }

#### goOnline

This api allows to set sequencer in online mode.

GoOnline
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #goOnline }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #goOnline-response }

#### goOffline

This api allows to set sequencer in offline mode.

GoOffline
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #goOffline }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #goOffline-response }


#### diagnosticMode

This api executes diagnostic handler from script

DiagnosticMode
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #diagnosticMode }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #diagnosticMode-response }

#### operationsMode

This api executes operations mode handler from script

OperationsMode
:   @@snip [sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http) { #operationsMode }

200 OK
:   @@snip [sequencer-embedded-server-response.http](../../../tools/sequencer/sequencer-embedded-server-response.http) { #operationsMode-response }

@@@ note

All the requests to sequencer server can result in `400 Bad Request` or `500 Internal Server error`

@@@  

## Source code for examples

* @github[sequencer-embedded-server.http](../../../tools/sequencer/sequencer-embedded-server.http)

@@@ note

The `sequencer-embedded-server.http` has first class support to execute http requests directly from IDEs like `IntelliJ IDEA` provided sequencer
is already running and all pre-requisites are met.

@@@
