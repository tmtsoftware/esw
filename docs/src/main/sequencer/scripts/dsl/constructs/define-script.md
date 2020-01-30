# Defining Script
 
There are 3 variations of Sequencer Scripts. These variations are based on the definition of script,
and the way of execution. The variations are:

- Regular Script
- Finite State Machine Script (FSM Script)
- Reusable Script

## Regular Script

Regular Script is a collection of declarations of @ref:[Script Handlers](handlers.md). To define a regular script,
a function named `script` needs to be invoked with a *block* which contains the core logic of the script.
The below example shows way to declare the script.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #script }
 
The *block* have 2 types of statements.

- Script Handlers: Will be executed when a command to execute a particular handler is received.
- Top-level statements (initialisation logic) : Will be executed while loading (initialising) this script,  

@ref:[Script Handlers](handlers.md) are defined to process Command Sequences or to perform actions like Going online or offline, starting Diagnostic mode etc.
Documentation of handlers can be found @ref:[here](handlers.md). Handlers will be executed whenever there is need to execute Sequence or to perform any action
on Sequencer.

Everything except Script Handlers are considered as top-level statements and will be executed while loading (initialising) the script.
This is the place to declare the Script specific variables and tasks to be executed at initialisation of the Script.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #script-example }

The example mainly demos:

- Top-level statements like declaring Script specific variable ( *tromboneTemperatureAlarm* ) , use of @ref:[Script Constructs](../script-constructs.md) ( *loopAsync* ) and
use of @ref:[Csw Services](../script-constructs.md) ( *info* - @ref:[Logging Service](../services/logging-service.md),
*setSeverity* - @ref:[Alarm Service](../services/alarm-service.md))
- Defining @ref:[Script Handlers](handlers.md) like *onSetup* , *onObserve* using @ref:[Csw Services](../script-constructs.md).

## Finite State Machine Script (FSM Script)

## Reusable Script

Reusable Scripts make it possible to write the common logic which needs to shared across multiple scripts. This can be used to build small building blocks for building
Sequencer Scripts.
 
They are same as the @ref:[Regular Script](#regular-script) except they cannot be directly loaded into a Sequence Component, and can only be loaded into
other Sequencer Scripts.

The common logic mainly consists of Script handlers and the top-level statements(initialisation logic). The top-level statements will be executed while
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
