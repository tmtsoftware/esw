# Sequencer Command Service

A Sequencer can send commands to Assemblies and HCDs and Sequences to other Sequencers.
In order to interact or send commands from one Sequencer to the other, one needs to create a `Sequencer` instance first.
The API provided by a Sequencer Command Service is tailored to Sequencer to Sequencer functionality.

##Sequencer

First a Sequencer instance is needed. To create a Sequencer instance, the following parameters need to be passed to the Sequencer method:

* `subsystem`: Subsystem of the Sequencer to be resolved and sent commands (for eg. TCS, IRIS)
* `observingMode`: observing mode of the Sequencer to be resolved and send commands to (for eg. wfos_imaging, wfos_spec)
* `defaultTimeout`: optional max timeout to wait for completion of Sequences sent with `sumbitAndWait` or `queryFinal`.  The default
value for this option is set to 10 hours since it will be common that the handling of Sequences can take a long time, and 
we don't want unexpected timeouts to occur in production.  For development, it might make sense to set this to some smaller value.
This can always be overridden in the specific `submitAndWait` and `queryFinal` calls, when appropriate (see below).

Here is an example:

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #creating-sequencer }

and here is one showing the setting of the default timeout:

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #creating-sequencer-timeout }


@@@ note { title="Resolving a Component with Location Service" }
Since all the components in the TMT architecture are dynamic in nature, which implies they can be shutdown and spawned dynamically
on some other location, the Sequencer is resolved each time the Command Service DSL is used. It is possible to create a Sequencer
entity for a non-existent component, but a command to the component will fail because the component is resolved when the 
command is sent. 
@@@

## Submitting Sequences to a Sequencer & Querying the Response

### Creating a Sequence
Unlike Assemblies and HCDs, Sequencers send Sequences to other Sequencers.  A `Sequence` is a 
list of `SequenceCommand` type instances, each of which can be one of the `Setup`, `Observe` or `Wait` types. 
To create a Sequence, create individual `SequenceCommand` objects and then create a `Sequence` with the `sequenceOf` DSL method as shown below.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #creating-sequence }  

This example `Sequence` consists of two steps. The Sequencer sends the two step `Sequence` to the destination Sequencer and waits for it to complete, which means
both of the two commands/steps are executed and completed.  All `Sequence` steps must complete successfully for the `Sequence` to complete successfully. 
  
The API for Sequence is @extref[here](csw_javadoc:csw/params/commands/Sequence.html). The API for SequenceCommand is @extref[here](csw_scaladoc:csw/params/commands/SequenceCommand.html)

#### Submit a Sequence to a Sequencer

In order to send a `Sequence` to another Sequencer, you can use the `submit` or `submitAndWait` DSL method as shown in examples below. The
`submit` DSL method sends the Sequence and returns `Started` if the `Sequence` is started or `Invalid` if there is a reason it cannot be started.
The `query` and `queryFinal` DSL is provided to check the response of a submitted sequence.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #submitAndQuery }  

The `query` DSL method allows checking on the state of the `Sequence`. `query` returns the current response immediately, which could be either a final 
response (eg. `Completed`) or the `Started` response. The `runId` of the submitted `Sequence` can be obtained from the `SubmitResponse` returned by `submit`.
`query` is useful in the case where polling of the command is needed or the script needs to take 
other actions and periodically check for the completion of the `Sequence`.    

Note that if the `runId` is not present in the Sequencer or has been removed from the CRM, the response returned 
is an `Invalid` response with an `IdNotAvailableIssue`. 

Please refer to @ref:[SubmitResponse extension utilities](submit-response-extensions.md) for using helper methods on `SubmitResponse`.

By default, any negative `SubmitResponse`  (for e.g. `Invalid` or `Error`) is treated as a Script error.
Refer to @ref:[error handling](../constructs/handlers.md#error-handlers) section for more details.
Alternatively, if you do not want to escalate a negative `SubmitResponse`, you can use `submit` Api with `resumeOnError` flag.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #resumeOnError }  

#### QueryFinal

While `query` returns immediately, `queryFinal` will wait for the final response of the `Sequence` for the `defaultTimeout`
specified at the time of creation of the `Sequencer` instance.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #queryFinal }  

If you want to increase/decrease the `defaultTimeout`, you can use the other variation of the same `queryFinal` DSL method which takes a timeout.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #queryFinalWithTimeout }  

#### Sending a Sequence with submitAndWait

The `submitAndWait` DSL method is a combination of `submit` followed with `queryFinal`. If you are not interested in the initial response
but only in final response of the Sequence, the `submitAndWait` DSL method is more convenient. It submits the `Sequence` and waits for the final response.
If the `Sequence` was successfully `Started`, it will wait until the `defaultTimeout` specified at the time of creation of the
`Sequencer` instance.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #submitAndWait }  

If you want to increase/decrease the default timeout, you can use the other variation of the same DSL method which takes a timeout.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #submitAndWaitWithTimeout }  

## Handling Online and Offline

As with Assemblies and HCDs, a Sequencer can also indicate to another Sequencer to go offline or online using the Sequencer Command Service DSL.

### Sequencer Sends goOnline

This DSL method is used to send online/offline commands to other sequencers.
The Sequencer can go online only if it is currently in the offline state. If this command is received in any other
state apart from offline, an `Unhandled` response will be sent.

If the Sequencer is in the Offline state, and it receives the `goOnline` command, the @ref:[goOnline handlers](../constructs/handlers.md#online-and-offline-handlers) of the receiving Sequencer
will be called. In case the handlers fail, a `GoOnlineHookFailed` response would be sent, and the Sequencer remains in the previous state.
Otherwise, an `Ok` response is returned, and the Sequencer goes in online(idle) state.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #goOnline }  

### Sequencer Sends goOffline

Go offline command is received in 2 states only.

* If the Sequencer is Idle, which means it is not processing any sequence currently
* If the Sequencer is Loaded with a sequence

If this command is sent in any other state apart from these, an `Unhandled` response will be sent.
If the Sequencer is in idle/loaded state, and it receives the `goOffline` command, the @ref:[goOffline handlers](../constructs/handlers.md#online-and-offline-handlers)
of the receiving Sequencer will be called.
In case the handlers fail, a `GoOfflineHookFailed` response would be sent, resulting the Sequencer remains in the previous state.
Otherwise an `Ok` message is sent, and the Sequencer goes to offline state.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #goOffline }  

## Handling Diagnostic and Operations Mode

### Diagnostic Mode

The `diagnosticMode` command can be sent to Sequencers in all states and a `DiagnosticModeResponse` is returned. If the Sequencer script has defined
the @ref:[diagnostic mode handlers](../constructs/handlers.md#diagnostic-mode-handler), they will be called. If the handlers execute successfully,
an `Ok` response is sent; otherwise, the `DiagnosticHookFailed` response is sent.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #diagnosticMode }  

### Operations Mode

The `operationsMode` command returns a Sequencer in a diagnostic data mode to normal operation. `operationsMode` is accepted by Sequencers in all states 
and an `OperationsModeResponse` is returned to the sender. If the Sequencer has defined its @ref:[operations mode handlers](../constructs/handlers.md#operations-mode-handler), 
they will be called. If the handlers execute successfully, an `Ok` response is sent; otherwise, the `OperationsHookFailed` response is sent.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #operationsMode }  

## Aborting and Stopping a Sequence

Stopping and aborting are meant to handle early termination of observing sequences in instruments. Aborting indicates that the current observe
should stop as soon as possible and save the data if possible. Stop indicates that the current observe should stop at the end of the current step.
In both cases, the observation is over and no subsequent steps can be executed.

Stop and abort commands are accepted only if the Sequencer is in `InProgress` state, which means it is executing a sequence.
If this command is sent in any other state, an `Unhandled` response is returned. In all other cases, an `Ok` response is sent.

### Aborting a Sequence

On receiving the abort command in the `InProgress` state, the Sequencer will execute the @ref:[abort sequence handlers](../constructs/handlers.md#abort-sequence-handler)
and on completion of execution of handlers (whether successful or failed), the Sequencer will discard all the `pending` steps
and return an `Ok` response.  

Note that, abort of a sequence does not abruptly terminate the in-flight step(s) which are already under execution. It will discard only the
pending steps, and the sequence is finished gracefully after the inflight step(s) are finished although the script can take action to end the current
step immediately.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #abortSequence }  

### Stopping a Sequence

Stopping a Sequence is very similar to aborting. The only difference is that instead of abort handlers, the @ref:[stop handlers](../constructs/handlers.md#stop-handler)
are called.

Kotlin
: @@snip [SequencerCommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SequencerCommandServiceDslExample.kts) { #stopSequence }  
