# Sequencer Command Service

In order to interact or send commands from one sequencer to the other, one needs to create a `Sequencer` instance first
and then send commands to it.

To create a Sequencer instance, following parameters need to be passed:

* `subsystem`: Subsystem of the Sequencer to be resolved and send commands to (for eg. TCS, IRIS)
* `observingMode`: observing mode of the Sequencer to be resolved and send commands to (for eg. darknight, clearsky)
* `defaultTimeout`: max timeout to wait for responses of commands like `sumbitAndWait` or `queryFinal`

Kotlin
: @@snip [RichSequencer.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/RichSequencer.kts) { #creating-sequencer }

Since all the components in the TMT architecture are dynamic in nature, which implies they can be shutdown and spawned dynamically
on some other location, the sequencer is resolved each time on receiving a command with the provided `subsystem` and `observingMode`.

## Submitting Sequences to Sequencer & Querying response

### Creating Sequence

A @scaladoc[Sequence](csw/params/commands/Sequence) is a list of @scaladoc[SequenceCommand](csw/params/commands/SequenceCommand) which could
be one of `Setup`, `Observe` or `Wait`. To create a Sequence, `sequenceOf` dsl could be used as shown below.

Kotlin
: @@snip [RichSequencer.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/RichSequencer.kts) { #creating-sequence }  
  
### Submitting sequence and Querying response

#### Submit and Query

In order to send Sequences to other Sequencers, you can use `submit` or `submitAndWait` dsl as shown in examples below.
`query` and `queryFinal` dsl is provided to query response of the `submit`ted sequence.

Kotlin
: @@snip [RichSequencer.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/RichSequencer.kts) { #submitAndQuery }  

`query` returns the current response which could be either final response (eg. `Completed`) or intermediate response (eg. `Started`).

#### QueryFinal

Whereas `queryFinal` will wait for the final response of the sequence for the `defaultTimeout`
specified at the time of creation of the `Sequencer` instance. This dsl will never return an intermediate response.

Kotlin
: @@snip [RichSequencer.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/RichSequencer.kts) { #queryFinal }  

If you want to increase/decrease the `defaultTimeout`, you can use the other variation of the same dsl which takes a timeout.

Kotlin
: @@snip [RichSequencer.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/RichSequencer.kts) { #queryFinalWithTimeout }  

#### Submit and Wait

The `submitAndWait` dsl is a combination of `submit` and `queryFinal`. If you are not interested in initial/intermediate response
but only in final response of the Sequence, you can use this dsl. It submits the sequence and waits for the final response
if the sequence was successfully `Started`. It will wait till the `defaultTimeout` specified at the time of creation of the 
`Sequencer` instance.

Kotlin
: @@snip [RichSequencer.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/RichSequencer.kts) { #submitAndWait }  

If you want to increase/decrease the default timeout, you can use the other variation of the same dsl which takes a timeout.

Kotlin
: @@snip [RichSequencer.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/RichSequencer.kts) { #submitAndWaitWithTimeout }  


## Going online/offline

## Diagnostic and operations mode

## Aborting and Stopping Sequence