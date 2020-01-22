# Command Service

Command Service DSL is a Kotlin wrapper for the CSW Command Service module for sending commands to Assemblies or HCDs via scripts.
You can refer to detailed documentation of the Command Service provided by CSW @extref[here](csw:commons/command.html#commandservice).
This DSL exposes following APIs:

## Assembly

This DSL creates a Command Service DSL object for the Assembly with the provided prefix that can be used to send commands from a script, 
such as sending Setups or Observes or lifecycle methods e.g. goOnline, goOffline, lock Assembly etc. This API also takes a default timeout
which will be used for commands like submitAndWait, queryFinal etc.

This DSL takes following parameters:

* `prefix`: Prefix of assembly
* `defaultTimeout`: command response timeout to be used when not explicitly provided for command

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #assembly }

## HCD

This DSL creates a Command Service DSL object for the HCD with the provided prefix that can be used to send commands from a script, 
such as sending Setups or Observes or lifecycle methods e.g. goOnline, goOffline, lock Assembly etc. This API also takes a default timeout
which will be used for commands like submitAndWait, queryFinal etc.


This DSL takes following parameters:

* `prefix`: - Prefix of HCD
* `defaultTimeout`: - command response timeout to be used when not explicitly provided for command

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #hcd }

@@@ note
Since all the components in the TMT architecture are dynamic in nature, which implies they can be shutdown and spawned dynamically
on some other location, the Assembly/HCD is resolved each time the Command Service DSL is used.
@@@

## Command Service DSL

### Submit

This DSL allows you to use the `submit` API to send a command to the Assembly/HCD, which returns a `Started` response on successful validation.  In this case, 
the final response can be obtained with the `queryFinal` api.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-component }

#### Error handling

In many cases, any errors encountered in a script would likely cause the command (and therefore, sequence) to fail.  Most of the time,
not much can be done other than capture and report the error that occurred.  It is possible some remediation can be performed, but 
it is likely the sequence would need to run again.  For this reason, we have simplified the error handling of commands such that
errors from the Command Service DSL calls are recasted as exceptions, which can then be caught by error handlers global to the 
sequence command handler, or the entire script.
In this way, such error handling does not need to be repeated throughout the script for each command sent.

To add an error handler to a sequence command handler, extend the command handler block with a `.onError` block.  The `SubmitResponse` error is captured
in a `ScriptError` type and passed into the block.  This type contains a `reason` String explaining what went wrong.  If this block does not exist, 
the global error handler will be called.  See the page on @ref:[Script Handlers](../handlers.md) for more information.  After this block is called, the
command, and the sequence, terminate with an Error status.

Because of this mechanism, a `submit` (and other Command Service API calls) always returns a positive `SubmitResponse`.  
For `submit`, the two possible responses are `Started` and `Completed`.  
They can be handled using the `.onStarted` and `.onCompleted` methods, respectively.  These methods allow you to specify a block of code to be
called in each of those cases.  Alternatively, a Kotlin `when` can be used to perform pattern matching on the result.  An example of both are 
shown below, along with an example of an `onError` handler for the sequence command handler.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-component-on-error }

If you desire to handle errors manually on a per-command basis, the `resumeOnError` flag can be used. If this flag is set to true,
then script execution continues, and action is taken based on custom logic
in script by using an `.onFailed` method. You can still choose to terminate sequence using the `onFailedTerminate` utility.
This will cause similar behavior as when flag is not set by calling the `onError` or `onGlobalError` blocks and terminating the sequence,
if the `SubmitResponse` is some kind of error.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-component-error-resume }

### SubmitAndWait

This DSL allows you to submit a command to the Assembly/HCD and wait for the final response. A timeout can be specified, 
indicating the time which it will wait for getting final submit response.  If this time expires, the command will timeout, breaking script execution flow, 
and the sequence is terminated with failure. If timeout is not provided explicitly, then timeout provided while creating 
instance of Assembly/HCD is used as default timeout.  This command follows the same error handling semantics as `submit` as described above.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-and-wait-component }

### Query

This DSL allows you to query the status of `submit` command that has returned a `Started` response . This response contains a 
`runId` which can be used to identify the command to query.
This command follows the same error handling semantics as `submit` as described above.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #query-component }

### QueryFinal

This DSL allows querying for final response of a `submit` command that has returned a `Started` response . This response contains a 
`runId` which can be used to identify the command to query for its final response.
A timeout can be specified,  indicating the time which it will wait for getting final submit response.  If this time expires, 
the command will timeout, breaking script execution flow, and the sequence is terminated with failure. If timeout is not provided explicitly,
then the timeout provided while creating instance of Assembly/HCD is used. 
This command follows the same error handling semantics as `submit` as described above.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #query-final-component }

### SubscribeCurrentState

This DSL allows subscribing to the current state data of the Assembly/HCD. You can provide a list of state names to subscribe to. If not provided,
all current state values are subscribed to. This DSL takes callback (or lambda), which is called whenever a item in the list of subscribed values 
changes value.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #subscribe-current-state-component }

## Going online/offline mode

This is a Kotlin wrapper for putting an Assembly/HCD into Online or Offline mode. When an Assembly/HCD receives this command, its respective handlers are called. The detailed documentation
of Online/Fffline handlers for Assembly/HCD can be found @extref[here](csw:framework/handling-lifecycle.html#component-online-and-offline)

### goOnline

This DSL command puts an Assembly/HCD into Online mode. `goOnline` can be called from anywhere in script. The following example
shows a Sequencer sending the `goOnline` command to a downstream "Galil Assembly" when it receives a `goOnline` command.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #goOnline-component }

### goOffline

This DSL command puts an Assembly/HCD into Offline mode. `goOffline` can be called from anywhere in script. The following example
shows a Sequencer sending the `goOffline` command to a downstream "Galil Assembly" when it receives a `goOffline` command.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #goOffline-component }

## Operations mode and Diagnostic mode

### operationsMode

This DSL command puts an Assembly/HCD into Operations mode. `operationsMode` can be called from anywhere in script. The following example
shows a Sequencer sending the `operationsMode` command to a downstream "Galil Assembly" when it receives an `operationsMode` command.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #operations-mode-component }

### diagnosticMode

This DSL command puts an Assembly/HCD into Diagnostic data mode based on a hint at the specified `startTime`. `diagnosticMode` can be called from anywhere in script. The following example
shows a Sequencer sending the `diagnosticMode` command to a downstream "Galil Assembly" when it receives a `diagnosticMode` command.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #diagnostic-mode-component }

## Locking and unlocking

### lock

This DSL command locks an Assembly/HCD from a Sequencer script for the specified duration. When you lock an Assembly/HCD, the Sequencer sending the lock command
is designated as the source, which is the only component that can send commands to the locked component while locked. 
This DSL returns a `LockingResponse` which can be `LockAcquired` in the successful scenario or `AcquiringLockFailed` in case of failure.
This DSL also provides callbacks for `onLockAboutToExpire` and, `onLockExpired` where script writer can write custom logic. These callbacks are thread safe.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #lock-component }

### unlock

This DSL command unlocks an Assembly/HCD from a Sequencer script for the specified duration. Only the Sequencer who locked the Assembly/HCD 
can unlock it. This DSL returns a `LockingResponse` which can be `LockReleased` or `LockAlreadyReleased` in the successful scenario or `ReleasingLockFailed`
in case of failure.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #unlock-component }

## Source code for examples

* [Command Service Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts)
