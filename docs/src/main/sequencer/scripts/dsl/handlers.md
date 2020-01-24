# Script Handlers

The handling of sequence execution is done by defining "handlers" in the script.  This is done by using special handler functions,
described below.  There are handlers that can be created to process Setup commands, Observe commands, aborting and stopping a sequence,
putting the Sequencer in Online and Offline modes, putting the Sequencer in Diagnostic mode and back to Operations mode, 
a global error handler to catch all uncaught exceptions, and a shutdown handler to shut down the Sequencer.
Each of these handlers are described below, with a section on [how to handle exceptions](#error-handlers) after that.

## Command Handlers

### onSetup

This handler is used to handle a @extref[Setup](csw_scaladoc:csw/params/commands/Setup.html) command sent to this sequencer.
The handler takes two parameters:

1. **command name** which is matched against the sequence command sent. If the command name matches, corresponding block provided
is executed
2. **block** of code which contains logic to act on the Setup command.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onSetup }

In the block provided to this handler, all the CSW services (Event, Alarm, Time Service, etc) and control DSL (loop, par etc) are accessible.

### onObserve

This handler is used to handle an @extref[Observe](csw_scaladoc:csw/params/commands/Observe.html) command sent to this sequencer.
The handler takes two parameters:

1. **command name** which is matched against the sequence command sent, if the command name matches, corresponding block provided
is executed
2. **block** of code which contains logic to act on the Observe command.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onObserve }

## Online and Offline Handlers

### onGoOnline

On receiving the `goOnline` command, the onGoOnline handlers, if defined, will be called. Only if the handlers execute successfully,
will the sequencer become online. Hence, error handling for the block passed to onGoOnline needs to be taken care of.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onGoOnline }

### onGoOffline

On receiving the `goOffline` command, the onGoOffline handlers, if defined, will be called. Only if the handlers execute successfully,
will the sequencer become offline. Hence, error handling for the block passed to onGoOffline needs to be taken care of.
Offline handlers could be written to clear the sequencer state before going offline.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onGoOffline }

## Abort Sequence Handler

Abort handler could be used to perform any cleanup tasks that need to be done before the current
sequence is aborted. Note that, even if the handlers fail, the sequence will be aborted.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #abort }

## Stop Handler

This handler is provided to clear/save the sequencer state before stopping.
Note that, even if the handlers fail, the sequence will be stopped.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #stop }

## Shutdown Handler

This handler will be called just before the sequencer is shutdown.
Note that, even if the handlers fail, the sequencer will be shutdown.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #shutdown }

## Diagnostic Mode Handler

This handler can be used to perform actions that need to be done when the sequencer goes in the diagnostic mode.
The handler gets access to two parameters:

* **startTime:** UTC time at which the diagnostic mode actions should take effect
* **hint:** represents supported diagnostic data mode by the Sequencer

Sequencer can choose to publish any diagnostic data in this handler based on the hint received, and/or send diagnostic command to downstream components.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #diagnosticMode }

## Operations Mode Handler

This handler can be used to perform actions that need to be done when the sequencer goes in the operations mode.
Script writers can use this handler to stop all the publishing being done by the @ref:[diagnostic mode handler](#diagnostic-mode-handler),
and/or send operations mode command to downstream components.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #operationsMode }

## Error Handlers

In many cases, any errors encountered in a script would likely cause the command (and therefore, sequence) to fail.  Most of the time,
not much can be done other than capture and report the error that occurred.  It is possible to perform some remediation, but 
it is likely the sequence would need to run again.

For this reason, we have simplified the error handling of commands such that
any DSL APIs that essentially return a negative (for e.g. Error or Cancelled) `SubmitResponse` are recasted as exceptions, which can then be caught 
by error handlers that are global to the sequence command handler, or the entire script.
In this way, such error handling does not need to be repeated throughout the script for each command sent.

Script can error out in following scenarios:

1. **Script Initialization Error** : When construction of script throws exception then script initialization fails. In this scenario,
framework will log error cause. Sequencer will not start in this failure. One needs to fix the error and then load script again.

2. **Command Handlers Failure** : While executing sequence, @ref:[Command Handlers](#command-handlers) e.g. `onSetup` , `onObserve` can fail because of two reasons:

    1. handler throws exception or
    2. `Command Service` or `Sequencer Command Service` used to interact with downstream `Assembly/HCD/Sequencer`
    returns negative `SubmitResponse`. Negative `SubmitResponse` is by default considered as error. In this case of failure, sequence is terminated
    with failure.

3. **Handlers Failure** : This failure occurs when any of handlers other than Command Handlers fail (e.g. `OnGoOnline`, `onDiagnosticMode` etc.). In
this scenario, framework will log error cause. Sequence execution will continue.  

Script DSL provides following constructs to handle failure while executing script:

1. **onGlobalError** : This construct is provided for script writer. Logic in `onGlobalError` will be executed in case of all **Handlers Failure** including
**Command Handlers Failure** except @ref:[Shutdown Handler](#shutdown-handler). If `onGlobalError` handler is not provided by script then only logging of error cause is done by the framework.

Following example shows usage of `onGloablError`

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onGlobalError }

2. **onError** : This construct is specifically provided for **Command Handlers Failure**.
`onError` block can be written specifically for each `onSetup` and `onObserve` handler. 
The `SubmitResponse` error is captured in a `ScriptError` type and passed into `onError` the block.
This type contains a `reason` String explaining what went wrong. 
In case of failure, `onError` will be called first followed by `onGlobalError` and sequence will be terminated with failure. 
After the error handling blocks are called, the command and hence the sequence, terminate with an Error status.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onError-for-exception }

By default, negative `SubmitResponse` is considered as error.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onError-for-negative-response }

@@@ note
Error in all handlers **except Shutdown Handler** will execute error handler provided by script. If error handler is not provided, framework will
log error cause.
@@@
