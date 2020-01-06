# FSM

Scripts have ability to define and run Finite State Machine(FSM). FSM can transition between defined states and can be made 
reactive to events and commands.

## Define FSM

### Create FSM

To create an instance of FSM, a helper method `Fsm` is provided as shown in example. This method takes following parameters:

1. `name` of FSM for logging
2. `initial state` of the FSM
3. `block` having states of the FSM

Kotlin
:   @@snip [fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/fsm.kts) { #create-fsm }  

### Define state

For creating FSM, the third parameter is a block which is the place to define all the states. A method named `state` needs to be called
with parameters `name` of the state and the `block` of actions to be performed when transitioned to that state.

When multiple states with same name are added, the last defined state will consider and others will be discarded.

@@@ note
State names are **case-insensitive**.
@@@

Kotlin
:   @@snip [fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/fsm.kts) { #define-state }

### State transition

To transit between states, `become` method needs to be called with name of next `state`. This will change the state of the fsm to the given state 
and start executing block of next state. This will throw an `InvalidStateException` if provided state is not defined.

State transition should ideally be the **last call of state**, or should be **done with proper control flow** so that become is **not called multiple times**.

Kotlin
:   @@snip [fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/fsm.kts) { #state-transition }

### Complete Fsm

`completeFsm` marks the Fsm complete. Calling it will immediately **stop execution of the FSM** and next steps are ignored, so it should be called at the end of the state.    

Kotlin
:   @@snip [fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/fsm.kts) { #complete-fsm }

### Helper constructs 
1. `entry` : executes the given `block` only when state transition happens from a different state

    Kotlin
    :   @@snip [fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/fsm.kts) { #entry }

2. `on` : executes the given `block` if given `condition` is **true** or **no condition is given**

    Kotlin
    :   @@snip [fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/fsm.kts) { #on } 

3. `after` : executes the given `block` after the given interval 

    Kotlin
    :   @@snip [fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/fsm.kts) { #after }

## Start fsm

After creating instance of FSM, it needs to be explicitly started by calling `start` on it. This will start executing the initial state of fsm which is 
provided while creating instance of it.

Kotlin
:   @@snip [fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/fsm.kts) { #start-fsm }

## Wait for completion

As FSM has ability to be complete itself, `await` can be called on the instance to wait for it's completion. In the example control flow will be stop at the
`await` statement till the FSM is marked complete.

Kotlin
:   @@snip [fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/fsm.kts) { #await } 

## Reactive FSM

FSM can be made to react to changes in event and command parameters. Both the types are explained below in
 
1. Event variable
2. Command flag 
 
### Event variable

Event variables are the way to make fsm react to events getting published. For doing this, we need to create a `EventVariable` of a specific 
parameter key of a specific event key and bind instance of fsm to it.
Whenever any event is published on the key given event key, all the FSMs which are bound to that variable will be refreshed.

Event variables are of 2 types

1. `SystemVar` - for SystemEvent
2. `ObserveVar` - for ObserveEvent

It is possible to bind one FSM to multiple event vars and vise versa. Following examples shows how to create event variables and bind FSM to it. 

Kotlin
:   @@snip [fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/fsm.kts) { #event-var }

### CommandFlag

Command flag is a bridge which can used to pass `Parameters` to FSM from outside of it. Example shows how to create `CommandFlag` and bind it with
FSM. Setting the params in command flag will reevaluate the all the FSMs with provided params which are bound to that flag.
It is possible to bind one FSM to multiple command flags and vise versa.

Kotlin
:   @@snip [fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/fsm.kts) { #command-flag } 


### define variables in Fsm's scope
