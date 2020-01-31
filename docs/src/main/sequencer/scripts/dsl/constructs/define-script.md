# Defining Script
 
There are 3 variations of Sequencer Scripts. These variations are based the way the Script gets executed. The variations are:

- Regular Script
- Finite State Machine Script (FSM Script)
- Reusable Script

## Regular Script

Regular script is like a collection of @ref:[script handlers](handlers.md) which executes the handlers of requested action or command. 
To define a regular script, a function named `script` needs to be invoked with a *block* which contains the logic of the script.
The below example shows way to declare the script.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #script }
 
The logic can be divided into 2 parts:

- Top-level statements (initialisation logic) : Executed while loading (initialising) the script.
- Script Handlers: Executed when a command to execute a particular handler is received.

@ref:[Script handlers](handlers.md) are defined to process Sequence of Commands or to perform actions like Going online or offline, starting Diagnostic mode etc.
Documentation of handlers can be found @ref:[here](handlers.md). Handlers will be executed whenever there is need to execute Sequence or to perform any action
on Sequencer.

Everything except Script Handlers are considered as Top-level statements and will be executed while loading the script.
This is the **place to declare the Script specific variables** and tasks to be executed at initialisation of the Script.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #script-example }

The example mainly demos:

- Top-level statements like declaring Script specific variable ( *tromboneTemperatureAlarm* ) , use of @ref:[Script Constructs](../script-constructs.md) ( *loopAsync* ) and
use of @ref:[Csw Services](../csw-services.md) ( *info* - @ref:[Logging Service](../services/logging-service.md),
*setSeverity* - @ref:[Alarm Service](../services/alarm-service.md))
- Defining @ref:[Script Handlers](handlers.md) like *onSetup* , *onObserve* using @ref:[Csw Services](../script-constructs.md).

## Finite State Machine Script (FSM Script)

FSM script is a way of writing Sequencer Script as @link:[Finite State Machines (FSM)](https://en.wikipedia.org/wiki/Finite-state_machine), where execution of
Script Handler is dependent on the Current State of the Sequencer Script.   

To define FSM Script a function `FSMScript` needs to be called with the *initial state* to start script with, and the block containing Script logic.
The block contains initialisation logic and different states. 

In FSM Script, Script handlers can be defined in two scopes :

- Default scope - top-level scope of the Script 
- State scope - scope of a specific state.

The below code shows how to declare FSM Script and States. It also shows the scopes where handlers can be added.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #fsm-script }

Initialisation of the Script takes place by executing the top-level statements, and then executing the *initial state*. 
The **top-level scope is the place to declare variables** which can be used in Script. 

While defining handlers there is restriction about @ref:[Command handlers](handlers.md#command-handlers) that they can only be tied to State scope.
Other @ref:[Script handlers](handlers.md) except the Command handlers can be tied both scopes of FSM script.

To execute any action, corresponding handlers in current State scope will be executed first and then handlers in Default scope will be executed.
In case of a Command Sequence, if the current state does not handle Command which is being executed, the Sequence will be completed with Error with reason
`UnhandledCommandException`.

For state transition, `become` needs to called from the current state with *next state*. It will start evaluating the next state, and will execute further actions on the
next state. If the *next state* is not defined in Script, then an exception will be thrown saying *No state declaration found for state*.

It is also possible to pass Params from current state to the next state by passing them as last argument to the *become* function. The passed Params will be available as a
function parameter while defining any State.

In below example, `[[ 1 ]]` shows use of `become` to change state. where `[[ 2 ]]` shows how to pass Params while changing state. 
The ON state shows how to consumes the Params.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #fsm-script-become }

The State scope can have top-level statements and Script handlers. The State's top-level statements will be executed when state transition happens. So invoking *become*
will initialise the next state which includes calling the top-level statements. The State top-level can be used to declare variables limited to State scope which will 
last till state transition. After that, state will be cleared and next time it will be initialised again to default values. 

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #fsm-script-state }

In the example, `initialPos` and `moved` demos declaring State scoped variables. Whenever state transition happens to some other state and back to SETTING-UP state,
these variables will be reinitialised to its default values as defined in code.
*Transition to self will not reinitialise variables*.      

## Reusable Script

Reusable Scripts make it possible to write the **common logic which needs to shared across multiple scripts**. This can be used to create small building blocks for building
Sequencer Scripts.
 
They are same as the @ref:[Regular Script](#regular-script) except they cannot be directly loaded into a Sequence Component, and can only be loaded into
other Sequencer Scripts.

The common logic consists of Script handlers and the top-level statements(initialisation logic). The top-level statements will be executed while
loading the script. Script handlers will be added to the corresponding handlers of the script loading it.

Following code declares a Reusable Script with Observe Command Handler.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #reusable-script-example }

### Loading in Regular Script

To use Reusable Scripts, the Regular script needs to call function called `loadScript` with the instance of Reusable Script. Calling *loadScript* will initialise
the Reusable Script and then combine handlers of both scripts.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #load-script }

### Loading in FSM Script

A Reusable Script cannot be directly imported at top-level of FSM script. It can only be imported in a particular State of the FSM script.
Loaded script is limited to that particular State. Below example loading script into a State.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #load-script-fsm }
