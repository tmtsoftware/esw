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
- Top-level statements: Will be executed while loading this script

@ref:[Script Handlers](handlers.md) are defined to process Command Sequences or to perform actions like Going online or offline, starting Diagnostic mode etc.
Documentation of handlers can be found @ref:[here](handlers.md). Handlers will be executed whenever there is need to execute Sequence or to perform any action
on Sequencer.

Everything except Script Handlers are considered as top-level statements and will be executed while loading the script.
This is the place to declare the Script specific variables and tasks to be executed at initialisation of the Script.

Kotlin
:   @@snip [define-script.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DefineScriptExample.kts) { #script-example }

The example mainly demos:

- top-level statements like declaring Script specific variable (*tromboneTemperatureAlarm*) and use of @ref:[Script Constructs](../script-constructs.md) (*loopAsync*).
- Using script handlers like *onSetup*, *onObserve*.

## Finite State Machine Script (FSM Script)

## Reusable Script
