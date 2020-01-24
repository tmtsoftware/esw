# Command Service for Assemblies and HCDs 

A Sequencer script can send commands to Assemblies and HCDs.
This section describes the Command Service DSL that is a wrapper for the CSW Command Service module for sending commands to Assemblies or HCDs within scripts.
You can refer to detailed documentation of the Command Service provided by CSW @extref[here](csw:commons/command.html#commandservice).

The DSL provides a way to define an Assembly or HCD as an object. This object encapsulates the Location Service and Command
Service of CSW to provide a higher level DSL for script usage. This DSL exposes following APIs:

A Sequencer can also send Sequences to other Sequencers. See @ref:[here](./sequencer-command-service.md) for more information on sending Sequences to Sequencers.

## Assembly
    
The Assembly DSL method creates a Command Service entity for an Assembly with the provided `Prefix` that can be used to send commands from a script, 
such as sending Setups or Observes or lifecycle methods e.g. goOnline, goOffline, lock Assembly etc. This DSL method provides a default timeout
which will be used for commands like submitAndWait, queryFinal etc, but also allows adding an Assembly-specific default timeout. The built-in default timeout
is 10 seconds. Commands requiring a timeout also allow command-specific timeouts.

Assembly takes the following parameters:

* `prefix`: Prefix of the Assembly as defined in the Assembly's model file
* `defaultTimeout`: optional command response timeout to be used when not explicitly provided for command

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #assembly }

## HCD

The HCD DSL method creates a Command Service DSL entity for an HCD with the provided `Prefix` that can be used to send commands from a script, 
such as sending Setups or Observes or lifecycle methods e.g. goOnline, goOffline, lock HCD etc. This DSL method provides a default timeout
which will be used for commands like submitAndWait, queryFinal etc., but also allows adding an HCD-specific default timeout. The built-in timeout
is 10.seconds. Commands requiring a timeout also allow command-specific timeouts.

HCD takes the following parameters:

* `prefix`: - Prefix of HCD as defined in the HCD's model file
* `defaultTimeout`: - optional command response timeout to be used when not explicitly provided for command

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #hcd }

@@@ note { title="Resolving a Component with Location Service" }
Since all the components in the TMT architecture are dynamic in nature, which implies they can be shutdown and spawned dynamically
on some other location, the Assembly/HCD is resolved each time the Command Service DSL is used. It is possible to create an Assembly
or HCD entity for a non-existent component, but a command to the component will fail because the component is resolved when the 
command is sent. 
@@@

## Command Service DSL

The Command Service API provided by the DSL for use in scripts is similar to the CommandService API provided by Scala or Java.
The big difference is that the DSL does not return a Future. In the scripting language, even though the commands are asynchronous,
each completes and returns its value, such as a `SubmitResponse`, not a `Future[SubmitResponse]`.
This section describes each of the available DSL methods. 

### Submit

The `submit` method of the DSL allows sending a `Setup` or `Observe` command to an Assembly/HCD. `submit` returns a positive
`SubmitResponse` which can be `Completed` or `Started`. A very short command may
quickly return `Completed`. A command that starts actions that are long-running returns `Started`. 

@@@ note { title="submit or submitAndWait?" }
`submit` is similar to `submitAndWait` in that both send a command to another component. `submit` is the right choice when
the command starts long-running actions, and you need to take additional actions before the command completes. A successful long-running
command returns a `Started` response that includes a `runId`, which can be used with `query` or `queryFinal` to wait for 
the commands final response at a later time.

`submitAndWait` combines `submit` and `queryFinal` as a shortcut when you only need to wait for all started actions to complete
before taking the next script step.
@@@

The following example shows a `submit` to the Galil Assembly that is going to take a long time.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-component }

#### Error Handling in Scripts

In most cases errors encountered in the execution of a script will likely cause the command (and therefore, Sequence) to fail.  Most of the time,
not much can be done when an error occurs other than to report the error that occurred.  In some cases, it is possible some remediation can be performed, but 
it is likely the Sequence would need to run again.  For this reason, the error handling of commands in a script has been simplified such that
errors from the Command Service DSL calls are captured and delivered to error handlers specific to a single 
sequence command handler, or global to the entire script. In this way, such error handling does not need to be repeated throughout the script for each command sent.

To add an error handler to a command handler, extend the command handler block with a `.onError` block.  The `SubmitResponse` error is captured
in a `ScriptError` type and passed into the block.  This type contains a `reason` String explaining what went wrong.  If the command handler does not have
an `onError` block, the global error handler will be called.  See the page on @ref:[Script Handlers](../constructs/handlers.md) for more information.
After this block is called, the command sending the sequence terminates with an Error status.

Because of this mechanism, a `submit` (and other Command Service API calls) always returns a positive `SubmitResponse`.  For 
`submit`, the two possible responses are `Started` and `Completed`.  They can be handled using the `.onStarted` 
and `.onCompleted` methods, respectively.  These methods allow you to specify a block of code to be
called in each of those cases.  Alternatively, a Kotlin `when` can be used to perform pattern matching on the result.  An example of both are 
shown below, along with an example of an `onError` handler for the sequence command handler. Since there are only two positive options, forming 
an if statement using the `isStarted` call on the `SubmitResponse` is convenient in many cases.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-component-on-error }

If you desire to handle errors manually on a per-command basis, the `resumeOnError` flag can be used. If this flag is set to true,
then script execution continues, and action is taken based on custom logic
in the script using an `.onFailed` method. You can still choose to terminate the Sequence using the `onFailedTerminate` utility.
This will cause similar behavior as when the flag is not set by calling the `onError` or `onGlobalError` blocks and terminating the sequence,
if the `SubmitResponse` is some kind of error.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-component-error-resume }

### SubmitAndWait

The `submitAndWait` DSL method combines `submit` and `queryFinal` allowing you to submit a command to 
an Assembly/HCD and wait for the final response. A timeout can be specified if needed, 
indicating the time `submitAndWait` will wait to receive the final `SubmitResponse`.  If this time expires, the command will timeout, breaking script execution flow, 
and the Sequence is terminated with failure. If timeout is not provided explicitly, then the timeout provided while creating the 
instance of Assembly or HCD is used as default timeout.  This command follows the same error handling semantics as `submit` as described above.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-and-wait-component }

@@@ note { title="Do I Need a Result Variable?" }
Note that this example does not save the `submitAndWait` result. If a `submitAndWait` does not return a result, and since the `submitAndWait` returns
only after the actions are completed, and errors are handled elsewhere, there is not much reason to bother with the result. 
If it is the case where the command returns a result in the `Completed`, save the returned `Completed` value and retrieve the result.
@@@ 

### Query

The `query` DSL method allows you to check the status of a `submit` command that has returned a `Started` response . The `Started` response contains a 
`runId` that can be used to identify the command to `query`. The `query` command returns immediately while `queryFinal` waits for the final response.
Therefore, `query` can be used to poll for the final response. Note that if the `runId` is not present or has been removed from the CRM, the response
returned is an `Invalid` response with an `IdNotAvailableIssue`. This command follows the same error handling semantics as `submit` as described above.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #query-component }

### QueryFinal

The `queryFinal` DSL method allows querying for the final response of a `submit` command that has returned a `Started` response . 
The `Started` response contains a `runId` that can be used to identify the command.
A timeout can be specified, indicating how long `queryFinal` wait for getting the final `SubmitResponse`.  If this time expires, 
the command will timeout, breaking script execution flow, and the sequence is terminated with failure. If timeout is not provided explicitly,
then the timeout provided while creating instance of Assembly/HCD is used. Note that if the `runId` is not present or has been removed from the CRM, 
the response returned is an `Invalid` response with an `IdNotAvailableIssue`. This command follows the same error handling semantics as `submit` as described above.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #query-final-component }

### SubscribeCurrentState

This DSL allows subscribing to the current state data of the Assembly/HCD. You can provide a list of state names to subscribe to. If not provided,
all current state values are subscribed to. This DSL takes a callback (or lambda), which is called whenever the Assembly/HCD publishes an item 
in the list of subscribed values.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #subscribe-current-state-component }

## Going To online/offline Mode

A Sequencer can command an Assembly or HCD to go to the online or offline state. This is a wrapper for putting another Assembly/HCD 
into Online or Offline mode. When an Assembly/HCD receives this command, its respective handlers are called. The detailed documentation
of Online/Fffline handlers for Assembly/HCD can be found @extref[here](csw:framework/handling-lifecycle.html#component-online-and-offline)

### goOffline

A declared Assembly/HCD includes a DSL command puts an Assembly/HCD into Offline mode. `goOffline` can be called from anywhere in script. This results in the
triggering of the `onGoOffline` handler in the component. The following example
shows a Sequencer sending the `goOffline` command to a downstream "Galil Assembly" when it receives a `goOffline` command.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #goOffline-component }

### goOnline

A declared Assembly/HCD includes a DSL command to put an Assembly/HCD into Online mode. `goOnline` can be called from anywhere in the script. This results in 
the triggering of the `onGoOnline` handler in the component. 
The following example shows a Sequencer sending the `goOnline` command to a downstream "Galil Assembly" when it receives a `goOnline` command.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #goOnline-component }


## Operations Mode and Diagnostic Mode

A Sequencer can place an Assembly or HCD in a diagnostic technical data mode. There are two methods in the Assembly/HCD Command Service DSL
related to technical data collection.

### diagnosticMode

The `diagnosticMode` DSL method puts an Assembly/HCD into Diagnostic data mode based on a hint at the specified `startTime`. `diagnosticMode` can be called from anywhere in script. 
The hint is specifified by the component. Not all components have diagnostic modes for technical data.  The following example
shows a Sequencer sending the `diagnosticMode` command to a downstream "Galil Assembly" when it receives a `diagnosticMode` command.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #diagnostic-mode-component }

### operationsMode

This `operationsMode` DSL method returns an Assembly/HCD to Operations mode, the normal running mode. `operationsMode` can be called from anywhere in script. 
The following example shows a Sequencer sending the `operationsMode` command to a downstream "Galil Assembly" when it receives an `operationsMode` command.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #operations-mode-component }

## Locking and Unlocking Assemblies and HCDs

A Sequencer script can lock and unlock individual Assemblies and HCDs. When a Sequencer locks a component, it is the only component that can send
commands to the component that will be accepted.

### lock

This Command Service DSL method locks an Assembly/HCD from a Sequencer script for the specified duration. When you lock an Assembly/HCD, the Sequencer sending the lock command
is designated as the source, which is the only component that can send commands to the locked component while locked. 
This DSL returns a `LockingResponse` which can be `LockAcquired` in the successful scenario or `AcquiringLockFailed` in case of failure.
This DSL also provides callbacks for `onLockAboutToExpire` and, `onLockExpired` where script writer can write custom logic. These callbacks are thread safe.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #lock-component }

### unlock

This Command Service DSL method unlocks an Assembly/HCD from a Sequencer script. Only the Sequencer that locked the Assembly/HCD 
can unlock it. This DSL returns a `LockingResponse` which can be `LockReleased` or `LockAlreadyReleased` in the successful scenario or `ReleasingLockFailed`
in case of failure.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #unlock-component }

## Source code for examples

* [Command Service Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts)
