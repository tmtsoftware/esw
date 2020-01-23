# Script Handlers

All the handlers which could be defined in various scopes are described below. 
Note that, in all the below described handlers, error-handling for the block passed to handlers, needs to be taken care by the
Script writers.

## Command Handlers

### onSetup

This handler is used to handle a @extref[Setup](csw_scaladoc:csw/params/commands/Setup.html) command sent to this sequencer.
The handler takes two parameters:

1. **command name** which is matched against the sequence command sent, if the command name matches, corresponding block provided
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

2. **onError** : This construct is specifically provided for **Command Handlers Failure**. `onError` block can be written specifically for each `onSetup` and
`onObserve` handler. In case of failure, `onError` will be called first followed by `onGlobalError` and sequence will be terminated with failure. By default
negative `SubmitResponse` is considered as error.

Following example shows command level error handler along with global
error handler. `onError` construct is available to handle failure of `onSetup` and `onObserve` command handler. In this example, submit
to assembly return negative `SubmitResponse` triggers error handling mechanism. 

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-component-on-error }

If you don't want to fail sequence in case of Command Service APIs while interacting with downstream Assembly/HCD (`submit`, `query` etc.)
or Sequencer Command Service APIs while interacting with downstream Sequencer (`submit`, `query` etc.) then **resumeOnError** flag can be used. For details of
**resumeOnError**, please refer @ref:[Error handling](./services/command-service.md#error-handling)

@@@ note
Error in all handlers **except Shutdown Handler** will execute error handler provided by script. If error handler is not provided, framework will
log error cause.
@@@
