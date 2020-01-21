# FSM

Scripts have ability to define and run Finite State Machine(FSM). FSM can transition between defined states and can be made 
reactive to Event and Command.

## Define FSM

### Create FSM

To create an instance of FSM, a helper method `Fsm` is provided as shown in example. This method takes following parameters:

1. `name` of FSM
2. `initial state` of the FSM
3. `block` having states of the FSM

Kotlin
:   @@snip [Fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #create-fsm }  

### Define state

As mentioned above, the third parameter of `Fsm` method is a block which is the place to define all the states of FSM. A method named `state` needs
 to be called with parameters `name` of the state and the `block` of actions to be performed in that state.   

@@@ note
1. State names are **case-insensitive**.
2. In case of multiple states with same name, the last one will be considered.
@@@

Kotlin
:   @@snip [Fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #define-state }

### State transition

To transit between states, `become` method needs to be called with name of next `state`. This will **change the state** of the fsm to the given state 
and **start executing next state**. `InvalidStateException` will be thrown if provided state is not defined.

Kotlin
:   @@snip [Fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #state-transition }

State transition should ideally be the **last call in state** or should be **done with proper control flow** so that become is **not called multiple times**.

### Complete FSM

`completeFsm` **marks the FSM complete**. Calling it will immediately **stop execution of the FSM** and next steps will be ignored, so it should be called at
the end of the state.    

Kotlin
:   @@snip [Fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #complete-fsm }

### Helper constructs 
1. `entry` : executes the given `block` only when **state transition happens from a different state**

    Kotlin
    :   @@snip [Fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #entry }

2. `on` : executes the given `block` if given `condition` is **true**

    Kotlin
    :   @@snip [Fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #on } 

3. `after` : executes the given `block` after the given `duration` 

    Kotlin
    :   @@snip [Fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #after }

## Start FSM

After creating instance of FSM, it needs to be **explicitly started** by calling `start` on it. This will **start executing the initial
 state** of fsm which is provided while creating instance of it.

Kotlin
:   @@snip [Fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #start-fsm }

## Wait for completion

As FSM has ability to be complete itself, `await` can be called to **wait for its completion**. **Control flow will be blocked** at the `await` statement
 till the FSM is marked complete.

Kotlin
:   @@snip [Fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #await } 

## Reactive FSM

FSM can be made to react to changes in Event and Command parameters with help of `Event variables` and `Command flags`.

**`bind`ing FSM to reactive variable is necessary** to achieve the reactive behavior of FSM. 
 
### Event variable

Event variables are the way to make fsm react to events. Event variable can be tied to only one Parameter Key in an event.
To make FSM react to Event variable, we need to create a `EventVariable` for a specific Parameter Key of an Event and **bind the FSM** to it.
FSM can be bind to multiple Event variables and vise versa.

Event variables use Event Service underneath, which makes it possible to share data between multiple sequencers.
Whenever any event is published on the key of given event, all the FSMs bound to that variable will be re-evaluated. 

Event variables are of 2 types:

1. `SystemVar` - are based on SystemEvent 
2. `ObserveVar` - are based on ObserveEvent

Event variable has capability to behave one of two ways
 - Subscribe to the Events getting published
 - Poll for a new event after every certain interval

#### Subscribe

Event variable subscribes to the given Event key and refresh the FSMs whenever an event is published. This will refresh the FSMs as soon as Event
is published.

Following examples shows how to create Event variables with subscribing behavior, `bind` FSM to it and methods like `get` and `set`. 
`set` will publish the event with modified parameter.

Kotlin
:   @@snip [Fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #event-var }

#### Poll

Polling behavior is for situations when it's **not necessary to refresh FSM on every Event**, can be done periodically after a certain duration.
Event variable polls to get the latest Event with given duration and if a new Event is published, it will refresh the FSMs.
Polling behavior can be used when the publisher is too fast and there is no need respond so quickly to it.

For creating Event variable with polling behavior, it needs an extra argument which is the `duration` to poll with, the example code demos it. 
Other methods like `get`, `set` and `bind` are same as shown @ref:[Subscribe](#subscribe) examples above.

Kotlin
:   @@snip [Fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #polling }

### CommandFlag

Command flag acts as bridge which can used to pass `Parameters` to FSM from outside. Setting the params in command flag will re-evaluate
the all the FSMs with provided params which are bound to that flag. It is possible to bind one FSM to multiple command flags and vise versa.
Command flag is limited to scope of a single script. It does not have any remote impact.

Example shows how to create `CommandFlag`, `bind` FSM to it and methods `get` and `set` which are provided to retrieve or set the value of
 params in command flag. 

Kotlin
:   @@snip [Fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #command-flag } 

## Example FSM

In the below example, `temparatureFsm` demonstrates how to define and use FSM in the scripts. The event variable is declared 
with event key `esw.temperature.temp` for param `temperature` and `temperatureFsm` is bind to it. The job of the `temperatureFsm` 
is to decide the `state` based on the `temperature` and publish it on event key `esw.temperatureFsm` with param key `state`. 

Logic of state change is: 
   
| condition |state |
| :---: | :---: |
|  temp == 30 |  FINISH |
|  temp > 40  |  ERROR  |
|  else       |  OK     |
    
Kotlin
:   @@snip [Fsm.kts](../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/FsmExample.kts) { #example-fsm }

Full example code is available [here]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/FsmExample.kts).

Key things in above example code are :
 
- `[[ 1 ]]`: Shows **top-level scope of the FSM which can used to declare variables** in FSM's scope and statements which should be executed while starting the FSM.
Statements written here will be executed only once when the FSM starts.
- `[[ 2 ]]`: The scope of the state. Statements written here will be executed on every refresh of the state, which makes this a **non-ideal place to declare variables**.
- `[[ 3 ]]`: State transitions from `OK` state to `ERROR`.
- `[[ 4 ]]`: Marks the FSM complete. Re-evaluation or state transitions cannot happen after this is executed.

Till point `[[ 4 ]]`, it's all about **defining the blue-print** and **initialising state of FSM** which includes executing statements at `[[ 1 ]]`.

- `[[ 5 ]]`: Shows the binding of `temperatureVar` and `temperatureFsm`. After this point, FSM will re-evaluate whenever events are published on `temperatureVar`.
- `[[ 6 ]]`: Starts **evaluating the initial state** of the FSM.
- `[[ 7 ]]`: Waits for completion of the FSM. In example, the script execution will be blocked till line `[[ 4 ]]` is executed which will mark the FSM complete. The script will 
continue execution after FSM is marked complete.

Example code also demos the use of the @ref:[helper constructs](#helper-constructs) like `entry`, `on`.