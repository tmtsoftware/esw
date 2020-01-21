# Sequencer Command Service

In order to interact or send commands from one Sequencer to the other, one needs to create a `Sequencer` instance first
and then send commands to it.

To create a Sequencer instance, following parameters need to be passed:

* `subsystem`: Subsystem of the Sequencer to be resolved and send commands to (for eg. TCS, IRIS)
* `observingMode`: observing mode of the Sequencer to be resolved and send commands to (for eg. darknight, clearsky)
* `defaultTimeout`: max timeout to wait for responses of commands like `sumbitAndWait` or `queryFinal`

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #creating-sequencer }

Since all the components in the TMT architecture are dynamic in nature, which implies they can be shutdown and spawned dynamically
on some other location, the Sequencer is resolved each time on receiving a command with the provided `subsystem` and `observingMode`.

## Submitting Sequences to Sequencer & Querying response

### Creating Sequence

A @extref[Sequence](csw_scaladoc:csw/params/commands/Sequence.html) is a list of @extref[SequenceCommand](csw_scaladoc:csw/params/commands/SequenceCommand.html) which could
be one of `Setup`, `Observe` or `Wait`. To create a Sequence, `sequenceOf` DSL could be used as shown below.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #creating-sequence }  
  
### Submitting sequence and Querying response

#### Submit and Query

In order to send Sequences to other Sequencers, you can use `submit` or `submitAndWait` DSL as shown in examples below.
`query` and `queryFinal` DSL is provided to query response of the `submit`ted sequence.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #submitAndQuery }  

`query` returns the current response which could be either final response (eg. `Completed`) or intermediate response (eg. `Started`).

#### QueryFinal

Whereas `queryFinal` will wait for the final response of the sequence for the `defaultTimeout`
specified at the time of creation of the `Sequencer` instance. This DSL will never return an intermediate response.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #queryFinal }  

If you want to increase/decrease the `defaultTimeout`, you can use the other variation of the same DSL which takes a timeout.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #queryFinalWithTimeout }  

#### Submit and Wait

The `submitAndWait` DSL is a combination of `submit` and `queryFinal`. If you are not interested in initial/intermediate response
but only in final response of the Sequence, you can use this dsl. It submits the sequence and waits for the final response
if the sequence was successfully `Started`. It will wait till the `defaultTimeout` specified at the time of creation of the 
`Sequencer` instance.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #submitAndWait }  

If you want to increase/decrease the default timeout, you can use the other variation of the same DSL which takes a timeout.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #submitAndWaitWithTimeout }  


## Going online/offline

### Go online

This DSL is used to send online/offline commands to other sequencers.
The Sequencer can go online only if it is currently in offline state. If this command is received in any other
state apart from offline, an `Unhandled` response will be sent.

If the Sequencer is in Offline state, and it receives the `goOnline` command, the @ref:[goOnline handlers](../handlers.md#online-and-offline-handlers) of the receiving sequencer
will be called. In case the handlers fail, a `GoOnlineHookFailed` response would be sent, resulting the Sequencer remains in the previous state.
Else an `Ok` message is sent, and the Sequencer goes in online(idle) state. 

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #goOnline }  

### Go offline

Go offline command is received in 2 states only.
 
* If the Sequencer is Idle, which means it is not processing any sequence currently
* If the Sequencer is Loaded with a sequence

If this command is sent in any other state apart from these, an `Unhandled` response will be sent. 
If the Sequencer is in idle/loaded state, and it receives the `goOffline` command, the @ref:[goOffline handlers](../handlers.md#online-and-offline-handlers)
of the receiving Sequencer will be called.
In case the handlers fail, a `GoOfflineHookFailed` response would be sent, resulting the Sequencer remains in the previous state.
Else an `Ok` message is sent, and the Sequencer goes to offline state. 

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #goOffline }  

## Diagnostic and operations mode

### Diagnostic mode

The diagnostic data mode command is accepted by Sequencers in all states and `DiagnosticModeResponse` is sent. If the Sequencer has defined
its @ref:[diagnostic mode handlers](../handlers.md#diagnostic-mode-handler), they will be called. If the handlers execute successfully,
an `Ok` response is sent else `DiagnosticHookFailed` response is sent.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #diagnosticMode }  

### Operations mode

Operations mode is accepted by Sequencers in all states and `OperationsModeResponse` is sent. If the Sequencer has defined
its @ref:[operations mode handlers](../handlers.md#operations-mode-handler), they will be called. If the handlers execute successfully,
an `Ok` response is sent else `OperationsHookFailed` response is sent.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #operationsMode }  


## Aborting and Stopping Sequence

### Aborting

This command is accepted only if the Sequencer is in `InProgress` state, which means it is executing a sequence currently. 
If this command is sent in any other state, an `Unhandled` response is returned. In all other cases, an `Ok` response is sent.

On receiving this command in `InProgress` state, the Sequencer will execute the @ref:[abort sequence handlers](../handlers.md#abort-sequence-handler)
and on completion of execution of handlers (whether successful or failed), the Sequencer will discard all the `pending` steps
and return an `Ok` response.  

Note that, abort sequence does not abruptly terminate the in-flight step(s) which are already under execution. It will discard only the
pending steps, and the sequence is finished gracefully after the inflight step(s) are finished.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #abortSequence }  

### Stopping

Stopping sequence is very similar to aborting, only difference is that instead of abort handlers, the @ref:[stop handlers](../handlers.md#stop-handler)
are called. Script writers are expected to save state of sequencer-script in the Stop handlers.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #stopSequence }  
