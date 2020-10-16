# Script Handlers

A Sequencer script processes Sequences by defining "handlers" in the script. This is done by completing the special handler functions
described below. There are handlers that can be created to process the Setup and Observe commands, which make up the Sequence,
but there are also handlers for specific reasons including: aborting and stopping a sequence,
putting the Sequencer in Online and Offline modes, and putting the Sequencer into a Diagnostic mode and back to Operations mode. 
There is also a global error handler to catch all uncaught exceptions, and a shutdown handler to perform cleanup befores the 
Sequencer shut down and exits.
Each of these handlers are described below, with a section on @ref:[how to handle exceptions](#error-handlers) after that.

## Command Handlers

### onSetup

This handler is used to handle a @extref[Setup](csw_javadoc:csw/params/commands/Setup.html) command sent to this Sequencer.
The handler takes two parameters:

1. **command name** which is matched against the sequence command sent. If the command name matches, corresponding block provided
is executed
2. **block** of code which contains logic to act on the Setup command.

In this `onSetup` example, commands are sent in parallel to each of the WFOS filter wheels. 

Kotlin
: @@snip [HandlersExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onSetup }

In the block provided to this handler, all the CSW services (Event, Alarm, Time Service, etc) and control DSL (loop, par etc) are accessible.

### onObserve

This handler is used to handle an @extref[Observe](csw_javadoc:csw/params/commands/Observe.html) command sent to this Sequencer.
The handler takes two parameters:

1. **command name** which is matched against the sequence command sent, if the command name matches, corresponding block provided
is executed
2. **block** of code which contains logic to act on the Observe command.

The following example imagines a WFOS Sequencer receiving an `Observe` that contains an exposureTime parameter. 
The exposureTime is extracted into a `Setup` that is sent to the detector Assembly to start the exposure.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onObserve }

## Online and Offline Handlers

### onGoOnline

On receiving the `goOnline` command, the onGoOnline handler, if defined, will be called. The Sequencer will become online only if the 
handler executes successfully.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onGoOnline }

### onGoOffline

On receiving the `goOffline` command, the onGoOffline handler, if defined, will be called. The Sequencer will become offline only if the 
handler executes successfully.  Offline handlers could be written to clear the sequencer state before going offline.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onGoOffline }

## Abort Sequence Handler

The abort handler could be used to perform any cleanup tasks that need to be done before the current
sequence is aborted (e.g. abort an exposure). Note that, even if the handlers fail, the sequence will be aborted.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #abort }

## Stop Handler

This handler is provided to clear/save the Sequencer state or stop exposures before stopping.
Note that, even if the handlers fail, the sequence will be aborted.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #stop }

## Shutdown Handler

This handler will be called just before the Sequencer is shutdown.
Note that, even if the handlers fail, the Sequencer will be shutdown.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #shutdown }

## Diagnostic Mode Handler

This handler can be used to perform actions that need to be done when the Sequencer goes in the diagnostic mode.
The handler gets access to two parameters:

* **startTime:** UTC time at which the diagnostic mode actions should take effect
* **hint:** represents the diagnostic data mode supported by the Sequencer

The Sequencer can choose to publish any diagnostic data in this handler based on the hint received, and/or send a diagnostic command to downstream components.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #diagnosticMode }

## Operations Mode Handler

This handler can be used to perform actions that need to be done when the Sequencer goes in the operations mode.
Script writers can use this handler to stop all the publishing being done by the @ref:[diagnostic mode handler](#diagnostic-mode-handler),
and/or send an operations mode command to downstream components.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #operationsMode }

## Error Handlers

In many cases, any errors encountered in a script would likely cause the command (and therefore, sequence) to fail.  Most of the time,
not much can be done other than capture and report the error that occurred.  It is possible to perform some remediation, but 
it is likely the sequence would need to run again.

For this reason, we have simplified the error handling of commands such that
any DSL APIs that essentially return a negative (e.g. Error or Cancelled) `SubmitResponse` are recasted as exceptions, which can then be caught 
by error handlers that are global to the sequence command handler, or the entire script.
In this way, such error handling does not need to be repeated throughout the script for each command sent.

A script can error out in following scenarios:

1. **Script Initialization Error** : When the construction of script throws exception then script initialization fails. In this scenario,
the framework will log the error cause. The Sequencer will not start on this failure. One needs to fix the error and then load script again.

2. **Command Handlers Failure** : While executing a sequence, @ref:[Command Handlers](#command-handlers) e.g. `onSetup` , `onObserve` can fail because of two reasons:

    1. handler throws exception or
    2. The `Command Service` or `Sequencer Command Service` used to interact with downstream `Assembly/HCD/Sequencer`
    returns negative `SubmitResponse`. A negative `SubmitResponse` is by default considered as error. In this case of failure, sequence is terminated
    with failure.

3. **Handlers Failure** : This failure occurs when any of handlers other than Command Handlers fail (e.g. `OnGoOnline`, `onDiagnosticMode` etc.). In
this scenario, framework will log the error cause. Sequence execution will continue.  

The Script DSL provides following constructs to handle failures while executing script:

### Global Error Handler

**onGlobalError** : This construct is provided for the script writer. Logic in the `onGlobalError` will be executed for all **Handler Failures** including
**Command Handler Failures** except the @ref:[Shutdown Handler](#shutdown-handler). If the `onGlobalError` handler is not provided by script,
then only the logging of error cause is done by the framework.

Following example shows usage of `onGlobalError`

Kotlin
: @@snip [HandlersExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onGlobalError }

@@@ note
Error in all handlers **except the Shutdown Handler** will execute the global error handler provided by script. If an error handler is not provided, the framework will
log the error cause.
@@@

### Error handling at command handler level 

**onError** : This construct is specifically provided for **Command Handler Failures**.
An `onError` block can be written specifically for each `onSetup` and `onObserve` handler. 
The `SubmitResponse` error is captured in a `ScriptError` type and passed to the `onError` block.
This type contains a `reason` String explaining what went wrong. 
In case of failure, `onError` will be called first followed by `onGlobalError` and the sequence will be terminated with failure. 
After the error handling blocks are called, the command and sequence, terminate with an Error status.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onError-for-exception }

By default, a negative `SubmitResponse` is considered an error.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onError-for-negative-response }

**retry**: This construct can be attached to an `onSetup` or `onObserve` handler to automatically retry
 the handler code in the case of **Command Handler Failures**.
A `retry` block expects a `retryCount` and optional parameter `interval` which specifies an interval after which `onSetup` or `onObserve` will be retried
in case of failure. The `retry` block can be used along with `onError` or it can be used independently. If `retry` is combined with `onError`, the `onError` block 
will be called before each retry attempt. If the command handler still fails after all retry attempts, the command fails with an Error status.  Then the 
`onGlobalError` block will be executed (if provided), and the sequence will be terminated with a failure as well (see @ref:[Global Error Handler](#global-error-handler).

The following example shows the `retry` construct used along with `onError`.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #retry }

The following example shows `retry` with an interval specified and used without an `onError` block.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #retry-with-interval }
