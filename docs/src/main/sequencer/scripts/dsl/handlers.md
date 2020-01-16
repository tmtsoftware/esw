# Script Handlers

All the handlers which could be defined in various scopes are described below. 
Note that, in all the below described handlers, error-handling for the block passed to handlers, needs to be taken care by the
Script writers.

## Command Handlers

### onSetup

This handler is used to handle a @extref[Setup](csw:csw/params/commands/Setup.html) command sent to this sequencer.
The handler takes two parameters:

1. **command name** which is matched against the sequence command sent, if the command name matches, corresponding block provided
is executed
2. **block** of code which contains logic to act on the Setup command.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onSetup }

In the block provided to this handler, all the CSW services (Event, Alarm, Time Service, etc) and control DSL (loop, par etc) are accessible.


### onObserve

This handler is used to handle an @extref[Observe](csw:csw/params/commands/Observe.html) command sent to this sequencer.
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
Script writers can use this handler to stop all the publishing being done by the [diagnostic mode handler](#diagnostic-mode-handler),
and/or send operations mode command to downstream components.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #operationsMode }


## Error Handlers

### Handling global errors

Script DSL provides `onGlobalError` handler where script writer can write logic like cleaning up of resources. This will be executed
before terminating sequence with failure.

Script execution can fail and go in error handler by following ways:

1. Handlers fail with exception.
2. Command Service and Sequencer Command Service APIs return negative `SubmitResponse` which is by default considered as error.

In both of above cases, error cause is logged by a framework and `onGlobalError` handler written in script is called. If `onGlobalError`
handler is not provided by script then only logging of error cause is done. After calling global error handler, script execution flow breaks
and sequence is terminated with failure.

Kotlin

: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onGlobalError }

### Handling errors at command level

If script writer wants to handle errors at command level, `onError` construct can be used. In this case, `onError` is called first, followed by `onGlobalError`
handler. This will also result in terminating sequence with failure. Following example shows command level error handler along with global
error handler. `onError` construct is available to handle failure of `onSetup` and `onObserve` command handler. Following example shows submit
to assembly return negative `SubmitResponse` triggers error handling mechanism. 

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-component-on-error }

If you don't want to fail sequence in case of Command Service APIs while interacting with downstream Assembly/HCD (`submit`, `query` etc.)
or Sequencer Command Service APIs while interacting with downstream Sequencer (`submit`, `query` etc.) then **resumeOnError** flag can be used. For details of
**resumeOnError**, please refer @ref:[Error handling](./services/command-service.md#error-handling) 
