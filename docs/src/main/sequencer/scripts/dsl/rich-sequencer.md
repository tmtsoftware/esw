# Interacting with Sequencer

In order to interact or send commands from one sequencer to other, one needs to create a `Sequencer` instance first
and then send commands to it. 

To create a Sequencer instance, following parameters need to be passed:

* `subsystem`: subsystem of the Sequencer to be resolved and send commands to
* `observingMode`: observing mode of the Sequencer to be resolved and send commands to
* `defaultTimeout`: max timeout to wait for responses of commands like `sumbitAndWait` or `queryFinal`

Kotlin
: @@snip [RichSequencer.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/RichSequencer.kts) { #creating-sequencer }

The sequencer is resolved each time on receiving a command with the provided `subsystem` and `observingMode`,
this is done to handle scenarios where a Sequencer might die after the Sequencer instance is created
and is probably spawned up on some other location.

## Submitting Sequences to Sequencer & Querying response

In order to send @scaladoc[Sequence](csw/params/commands/Sequence)s to other Sequencer, you can
use `submit` or `submitAndWait` Api as shown in examples below.

Kotlin
: @@snip [RichSequencer.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/RichSequencer.kts) { #submit }  

`query` or `queryFinal` Apis could be used for the sequence result after the sequence is `submit`ted.
`query` returns the current response which could be either final response (eg. `Completed`) or intermediate response (eg. `Started`).
Whereas `queryFinal` will wait for the final response of the sequence for the given `timeout`. This Api will never return an intermediate response.


The `submitAndWait` Api is a combination of `submit` and `queryFinal` Apis. If you are not interested in initial/intermediate response
but only in final response of the Sequence, you can use this api. It submits the sequence and waits for the final response
if the sequence was successfully `Started`.

Kotlin
: @@snip [RichSequencer.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/RichSequencer.kts) { #submitAndWait }  


## Going online/offline

## Diagnostic and operations mode

## Aborting and Stopping Sequence