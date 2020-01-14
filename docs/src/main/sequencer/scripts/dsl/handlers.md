# Script Handlers

## Command Handlers

### onSetup

This handler is used to handle a `Setup` command sent to this sequencer.
The handler takes two parameters:

1. **command name** which is matched against the sequence command sent, if the command name matches, corresponding block provided
is executed
2. **block** of code which contains logic to act on the sequence command.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onSetup }

In the block provided to this handler, all the csw services (event, alarm, time service, etc) and control dsl (loop, par etc) are accessible.


### onObserve

This handler is used to handle an `Observe` command sent to this sequencer.
The handler takes two parameters:

1. **command name** which is matched against the sequence command sent, if the command name matches, corresponding block provided
is executed
2. **block** of code which contains logic to act on the sequence command.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onObserve }


## Online and Offline Handlers

### onGoOnline

On receiving the `goOnline` command, the onGoOnline handlers, if defined, will be called. Only if the handlers execute successfully,
will the sequencer become online. Hence, error handling for the block passed to onGoOnline needs to be taken care of by the script writer.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onGoOnline }


### onGoOffline

On receiving the `goOffline` command, the onGoOffline handlers, if defined, will be called. Only if the handlers execute successfully,
will the sequencer become offline. Hence, error handling for the block passed to onGoOffline needs to be taken care of by the script writer.
Offline handlers could be written to clear the sequencer state before going offline.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onGoOffline }


## Abort Sequence Handler

## Stop Handler

## Shutdown Handler

## Diagnostic Mode Handler

## Operations Mode Handler

## exceptionHandlers