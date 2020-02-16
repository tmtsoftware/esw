# Defining A Script
 
There are 3 variations of Sequencer Scripts. These variations are based the way the Script gets executed. The variations are:

- Handler-Oriented Script
- State Machine-Oriented Script (FSM Script)
- Reusable Script

## Handler-Oriented Script

A handler-oriented script is a collection of @ref:[script handlers](handlers.md) which execute the actions tied to a command. 
To define a handler-oriented script, a function named `script` needs to be invoked with a code *block* which contains the logic of the script.
The example below shows the way to declare the script.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #script }
 
The logic can be divided into 2 parts:

- Top-level statements (initialization logic): Executed while loading (initializing) the script.
- Script Handlers: Executed when a command to execute a particular handler is received.

@ref:[Script handlers](handlers.md) are defined to process a Sequence of Commands or to perform actions like going online or offline, starting a diagnostic mode etc.
Documentation of handlers can be found @ref:[here](handlers.md). Handlers will be executed whenever the Sequence or outside client makes a request to perform an action
on the Sequencer.

Everything except Script Handlers are considered as top-level statements and will be executed while loading the script.
This is the place to **declare the Script specific variables** and tasks to be executed at initialization of the Script.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #script-example }

The example mainly demos:

- Top-level statements like declaring Script specific variable (*tromboneTemperature*, *tromboneTemperatureAlarm*) , use of @ref:[Script Constructs](../script-constructs.md) (*loopAsync*) and
use of @ref:[Csw Services](../csw-services.md) (*info* - @ref:[Logging Service](../services/logging-service.md),
*setSeverity* - @ref:[Alarm Service](../services/alarm-service.md))
- Defining @ref:[Script Handlers](handlers.md) like *onSetup* and *onObserve*

## State Machine-Oriented Script (FSM Script)

FSM script is a way of writing a Sequencer Script as a @link:[Finite State Machine (FSM)](https://en.wikipedia.org/wiki/Finite-state_machine), where execution of
Script Handler is dependent on the current state of the Sequencer Script.   

To define FSM Script a function `FSMScript` needs to be called with the *initial state* of the script, and a block containing the Script logic.
The block contains initialization logic for different states.

In an FSM Script, script handlers can be defined in two scopes :

- Default scope - top-level scope of the Script 
- State scope - scope of a specific state.

The below code shows how to declare FSM Script and States. It also shows the scopes where handlers can be added.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #fsm-script }

Initialization of the Script takes place by executing the top-level statements, and then executing the *initial state*. 
The **top-level scope is the place to declare variables** which can be used across all states in the Script. 

While defining handlers there are some restrictions about @ref:[Command handlers](handlers.md#command-handlers) and where they can be defined in the FSM script scope.
@ref:[Script handlers](handlers.md) other than the Command handlers can be tied both scopes of FSM script.

To execute any action, the corresponding handlers in the current state scope will be executed first and then handlers in the Default scope, if any, will be executed.
In case of a Command Sequence, if the current state does not handle the Command which is being executed, the Sequence will be completed with an Error with a 
`UnhandledCommandException` reason.

For state transitions, `become` needs to called from the current state with the *next state* as an argument. 
It will start evaluating and executing future actions in the next state. If the *next state* is not defined in the 
Script, then an exception will be thrown saying *No state declaration found for state*.

It is also possible to pass parameters from the current state to the next state by passing them as the last argument to 
the *become* function. The passed parameters will be available as a
function parameter while defining any State.  This can reduce the need for global variables.

In below example, `[[ 1 ]]` shows use of `become` to change state. where `[[ 2 ]]` shows how to pass Params while changing state. 
The ON state shows how to consume the Params.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #fsm-script-become }

The `state` scope can have top-level statements and Script handlers. The `state`'s top-level statements will be executed 
when state transition happens. So invoking *become* will initialize the next state which includes calling the top-level 
statements. The `state` top-level can be used to declare variables limited to the scope of the state which will 
last until state transition. After that, the state will be cleared, and the next time the FSM becomes that state, 
it will be initialized again to default values. 

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #fsm-script-state }

In the example, `initialPos` and `moved` demonstrate declaring State scoped variables. Whenever state transition happens 
to some other state and the goes back to the `SETTING-UP` state, these variables will be reinitialized to their default 
values as defined in code.  *A transition to self will not reinitialize variables*.      

## Reusable Scripts

Reusable Scripts make it possible to write any **common logic that can shared across multiple scripts**. This can be 
used to create small building blocks for constructing Sequencer Scripts (although this does cause dependencies that 
reduce a script's ability to stand alone).  They cannot be directly loaded  into a Sequence Component; they can only 
be loaded into other Sequencer Scripts.

Reusable Scripts may consist of Script handlers or the top-level statements (initialization logic). The top-level 
statements will be executed while loading the script. Script handlers will be added to the corresponding handlers 
of the script loading it.

THe following code declares a Reusable Script with an Observe Command Handler.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #reusable-script-example }

### Loading in a Handler-Oriented Script

To use Reusable Scripts, a Handler-Oriented script needs to call the `loadScripts` function with the instance of Reusable 
Script. Calling *loadScripts* will initialize the Reusable Script and then combine handlers of both scripts.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #load-script }

### Loading in FSM Script

A Reusable Script cannot be directly imported at the top-level of an FSM script. It can only be imported in a 
particular State of the FSM script.  THe `loadScripts` command and the logic from the loaded script is limited to that particular State. 
The example below shows loading a reusable script into a State.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #load-script-fsm }


