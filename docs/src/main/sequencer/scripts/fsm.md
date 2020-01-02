# FSM

Scripts have ability to define and run Finite State Machine(FSM). FSM can transition between defined states and can be made 
reactive to any published events or commands.

## Basic functionality
### Create FSM

To create instance of FSM, a helper method `Fsm` is provided as shown in example. This method takes following parameters:

1. `name` of FSM for logging
2. `initial state` of the FSM
3. `block` having states of the FSM

Kotlin
:   @@snip [fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/fsm.kts) { #create-fsm }  

### Define state

For creating fsm, the third parameter is a block which is the place to define all the states. A method named `state` needs to be called
with parameters `name` of the state and the `block` of actions to be performed when transitioned to that state.
When multiple states with same name are added, the last defined state will considered and others will be discarded.

Kotlin
:   @@snip [fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/fsm.kts) { #define-state }

@@@ note
State names are **case insensitive**.
@@@

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

### entry 
### on 
### after
### start fsm
### await for completion

### EventVar
### CommandFlag

### define variables in Fsm's scope
