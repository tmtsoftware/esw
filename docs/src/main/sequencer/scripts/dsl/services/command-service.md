# Command Service

Command service dsl is kotlin wrapper over csw command service module provided for sending commands to assemblies or hcds via scripts.
This dsl exposes following APIs:

## Assembly

This dsl resolves assembly with provided name and gives handle to command service dsl through which script can interact with
assembly. For example send commands or lifecycle methods e.g. goOnline, goOffline, lock assembly etc. This api also takes default timeout
which will be used in commands like submitAndWait, queryFinal etc.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #assembly }

## Hcd

This dsl resolves hcd with provided name and gives handle to command service dsl through which script can interact with hcd. For example
send commands to assembly or lifecycle methods e.g. goOnline, goOffline, lock hcd etc.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #hcd }

@@@ note
Following dsl can be used to interact with both **Assembly and HCD** resolved using APIs explained above.
@@@

## Submit

This dsl allows to submit a command to assembly/hcd and return after first phase. If it returns Started then final response can
be obtained with query final api.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-component }

### Error handling
submit always returns positive submit response. In case of negative submit response, onError handler (if written) is called and then
onGlobalError handler is called. Script execution flow breaks in case of negative submit response and sequence is terminated with failure. 
Following example shows scenario where script execution flow breaks when submit return negative response, in this case onError handler will be executed followed by
onGlobalError handler, and sequence is completed with failure.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-component-on-error }

To change this default behaviour, resumeOnError flag can be used. If this flag is set to true then script execution continues, and action is taken based on custom logic
in script. Script writer can still choose to terminate sequence using `failedOnTerminate` utility.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-component-error-resume }

## SubmitAndWait

This dsl allows submitting command to assembly/hcd to submit command to assembly/hcd and waits for positive final response. Script writer can provide a timeout
for which it will wait for getting final submit response, otherwise command will timeout and script execution flow breaks and sequence is terminated with failure.
If timeout is not provided explicitly, then timeout provided while creating instance of assembly/hcd is used as default timeout. 

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-and-wait-component }

submitAndWait always return positive final response. In case of negative response it follows same error handling semantics as submit explained above. 

## Query

This dsl allows querying for response of submitted command. Started response returned by submit has runId which can be used to query for response.
Query always returns positive submit response. In case of negative response it follows same error handling semantics as submit.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #query-component }

## QueryFinal

This dsl allows querying for final response of submitted command. Started response returned by submit has runId which can be used to query for final response.
Script writer can provide a timeout for which it will wait for getting final submit response, otherwise command
will timeout and script execution flow breaks and sequence is terminated with failure. If timeout is not provided explicitly,
then timeout provided while creating instance of assembly/hcd is used as default timeout. 
QueryFinal always returns positive final response. In case of negative response it follows same error handling semantics as
submit. 

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #query-final-component }

@@@ note
submit, submitAndWait, query and queryFinal always return positive response. In case of negative response (considered as error by default), 
script execution flow breaks, error handling mechanism kicks in and sequence is terminated with failure. **resumeOnError**
allows to change this default behaviour and custom logic in script can decide flow. For details of error handling in script,
please refer @ref:[Error handling in script](../error-handling.md)
@@@

## SubscribeCurrentState

This dsl allows subscribing to current states of assembly/hcd. Script writer can provide state names to subscribe. If not provided
all current states are subscribed. This dsl takes callback, callback provides handle to subscribed state and script writer can write logic in 
callback which will be executed for all subscribed states.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #subscribe-current-state-component }

## goOnline

This dsl allows to send assembly/hcd into online mode. `goOnline` can be called from anywhere in script. Following example 
shows sequencer sending `goOnline` command to downstream galil assembly when it receives `goOnline` command. 

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #goOnline-component }

## goOffline

This dsl allows to send assembly/hcd into offline mode. `goOffline` can be called from anywhere in script. Following example 
shows sequencer sending `goOffline` command to downstream galil assembly when it receives `goOffline` command. 

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #goOffline-component }

## operationsMode

This dsl allows to send assembly/hcd into operations mode. `operationsMode` can be called from anywhere in script. Following example 
shows sequencer sending `operationsMode` command to downstream galil assembly when it receives `operationsMode` command. 

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #operations-mode-component }

## diagnosticMode

This dsl allows to send assembly/hcd into diagnostic data mode based on a hint at the specified startTime. `diagnosticMode` can be called from anywhere in script. Following example 
shows sequencer sending `diagnosticMode` command to downstream galil assembly when it receives `diagnosticMode` command. 

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #diagnostic-mode-component }

