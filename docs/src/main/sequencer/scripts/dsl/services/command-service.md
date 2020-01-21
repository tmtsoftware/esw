# Command Service

Command Service DSL is kotlin wrapper over CSW Command Service module provided for sending commands to assemblies or HCDs via scripts.
You can refer a detailed documentation of Command Service provided by CSW @extref[here](csw:commons/command.html#commandservice).
This DSL exposes following APIs:

## Assembly

This DSL creates Assembly instance with provided name and gives handle to Command Service DSL through which script can interact with
assembly. For example send commands or lifecycle methods e.g. goOnline, goOffline, lock Assembly etc. This api also takes default timeout
which will be used in commands like submitAndWait, queryFinal etc.

This DSL takes following parameters:

* `prefix`: Prefix of assembly
* `defaultTimeout`: if DSL like submitAndWait, queryFinal etc does not explicitly provide timeout then this Default timeout is used.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #assembly }

## HCD

This DSL creates HCD instance with provided name and gives handle to Command Service DSL through which script can interact with HCD. For example
send commands to Assembly or lifecycle methods e.g. goOnline, goOffline, lock HCD etc. This api also takes default timeout which will be used in commands
like submitAndWait, queryFinal etc.

This DSL takes following parameters:

* `prefix`: - Prefix of assembly
* `defaultTimeout`: - if DSL like submitAndWait, queryFinal etc does not explicitly provide timeout then this Default timeout is used.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #hcd }

@@@ note
Since all the components in the TMT architecture are dynamic in nature, which implies they can be shutdown and spawned dynamically
on some other location, the assembly/hdc is resolved each time on receiving a command with the provided `prefix`.
Following DSL can be used to interact with both **Assembly and HCD** resolved using APIs explained above.
@@@

## Command Service Dsl

### Submit

This DSL allows to submit a command to Assembly/HCD and return after first phase. If it returns Started then final response can
be obtained with query final api.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-component }

#### Error handling

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

### SubmitAndWait

This DSL allows submitting command to Assembly/HCD to submit command to Assembly/HCD and waits for positive final response. Script writer can provide a timeout
for which it will wait for getting final submit response, otherwise command will timeout and script execution flow breaks and sequence is terminated with failure.
If timeout is not provided explicitly, then timeout provided while creating instance of Assembly/HCD is used as default timeout. 

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-and-wait-component }

submitAndWait always return positive final response. In case of negative response it follows same error handling semantics as submit explained above. 

### Query

This DSL allows querying for response of submitted command. Started response returned by submit has runId which can be used to query for response.
Query always returns positive submit response. In case of negative response it follows same error handling semantics as submit.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #query-component }

### QueryFinal

This DSL allows querying for final response of submitted command. Started response returned by submit has runId which can be used to query for final response.
Script writer can provide a timeout for which it will wait for getting final submit response, otherwise command
will timeout and script execution flow breaks and sequence is terminated with failure. If timeout is not provided explicitly,
then timeout provided while creating instance of Assembly/HCD is used as default timeout. 
QueryFinal always returns positive final response. In case of negative response it follows same error handling semantics as
submit. 

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #query-final-component }

@@@ note
submit, submitAndWait, query and queryFinal always return positive response. In case of negative response (considered as error by default), 
script execution flow breaks, error handling mechanism kicks in and sequence is terminated with failure. **resumeOnError**
allows to change this default behaviour and custom logic in script can decide flow. For general guidelines of error handling in script,
please refer @ref:[Error handling in script](../handlers.md#error-handlers)
@@@

### SubscribeCurrentState

This DSL allows subscribing to current states of Assembly/HCD. Script writer can provide state names to subscribe. If not provided
all current states are subscribed. This DSL takes callback, callback provides handle to subscribed state and script writer can write logic in
callback which will be executed for all subscribed states.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #subscribe-current-state-component }

## Going online/offline mode

This is kotlin wrapper for sending Assembly/HCD in online and offline mode. When Assembly/HCD receives this command respective handlers are called. The detailed documentation
of online/offline handlers for Assembly/HCD can be found @extref[here](csw:framework/handling-lifecycle.html#component-online-and-offline)

### goOnline

This DSL allows sending Assembly/HCD into online mode. `goOnline` can be called from anywhere in script. Following example
shows Sequencer sending `goOnline` command to downstream galil Assembly when it receives `goOnline` command.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #goOnline-component }

### goOffline

This DSL allows to send Assembly/HCD into offline mode. `goOffline` can be called from anywhere in script. Following example
shows Sequencer sending `goOffline` command to downstream galil Assembly when it receives `goOffline` command.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #goOffline-component }

## Operations mode and Diagnostic mode

### operationsMode

This DSL allows to send Assembly/HCD into operations mode. `operationsMode` can be called from anywhere in script. Following example
shows Sequencer sending `operationsMode` command to downstream galil Assembly when it receives `operationsMode` command.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #operations-mode-component }

### diagnosticMode

This DSL allows to send Assembly/HCD into diagnostic data mode based on a hint at the specified startTime. `diagnosticMode` can be called from anywhere in script. Following example
shows Sequencer sending `diagnosticMode` command to downstream galil Assembly when it receives `diagnosticMode` command.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #diagnostic-mode-component }

## Locking and unlocking

### lock

This DSL allows locking Assembly/HCD from Sequencer script for specified duration. When you lock Assembly/HCD, Sequencer sending lock command
is treated as source. This DSL returns `LockingResponse` which can be `LockAcquired` in the successful scenario or `AcquiringLockFailed` in case of failure.
This DSL also provides callbacks for `onLockAboutToExpire` and, `onLockExpired` where script writer can write custom logic. These callbacks are thread safe.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #lock-component }

### unlock

This DSL allows unlocking Assembly/HCD from Sequencer script for specified duration. When you unlock Assembly/HCD, Sequencer sending lock command
is treated as source. This DSL returns `LockingResponse` which can be `LockReleased` or `LockAlreadyReleased` in the successful scenario or `ReleasingLockFailed`
in case of failure.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #unlock-component }

## Source code for examples

* [Command Service Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts)
