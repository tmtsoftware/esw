# Finite State Machines

Scripts have ability to define, include, and run @link:[Finite State Machine (FSM)](https://en.wikipedia.org/wiki/Finite-state_machine). 
A FSM can transition between defined states and can be made reactive to Events and Commands.

## Define a FSM

### Create the FSM

To create an instance of an FSM, a helper method `Fsm` is provided as shown in example. This method takes following parameters:

1. `name` of FSM
2. `initial state` of the FSM
3. `block` having states of the FSM

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #create-fsm }  

### Define State

As mentioned above, the third parameter of `Fsm` method is a block which is the place to define all the states of the FSM. A method named `state` needs
to be called with parameters `name` of the state and the `block` of actions to be performed in that state.

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #define-state }

@@@ note {title="State names" }

1. State names are **case-insensitive**.
2. In case of multiple states with same name, the last one will be considered.

@@@

### State Transition

To transition between states, the `become` method needs to be called with name of *next state*. This will **change the state of the FSM to the next state
and start executing it**. `InvalidStateException` will be thrown if the provided next state is not defined.

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #state-transition }


@@@ warning {title='Caution with Become'}
State transition should ideally be the **last call in state** or should be **done with proper control flow** so that become is **not called multiple times**.
@@@

Along with changing state, it is also possible to pass *Params* from the current state to the next state. Params can be given to *become* as last argument, which will 
then be injected in the next state as a parameter.

In a case where **state transition does not happen** while executing a state, the **FSM will stay in the same state** and any re-evaluation of the FSM after that will execute
the same state until a state transition happens. The @ref:[reactive variables](#reactive-fsm) plays an important role in this as they are the way to
re-evaluate the FSM state.

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #state-transition-on-re-evaluation }

In the example above, the FSM is in LOW state. If the temperature is below 20, then there won't be any state transition which will keep the FSM in same LOW state.
Change in temperature after that will re-evaluate the "LOW" state again and if the temperature is greater than or equal to 20 then current state will change to HIGH.
In the example `temperature` is an @ref:[event variable](#event-variables) which enables the re-evaluation of current state on changes in temperature value.

### Complete FSM

`completeFsm` **marks the FSM as complete**. Calling it will immediately **stop execution of the FSM** and next steps will be ignored. Therefore, it should be called at
the end of a state.

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #complete-fsm }

### FSM Helper Constructs

The following are some useful FSM constructs. 

1. `entry` : executes the given `block` only when **state transition happens from a different state**

    Kotlin
    :   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #entry }

2. `on` : executes the given `block` if the given `condition` evaluates to **true**. This construct should be used for conditional execution of a task.

    Kotlin
    :   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #on }

3. `after` : executes the given `block` after the given `duration`

    Kotlin
    :   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #after }

## Start FSM

After creating instance of FSM, it needs to be **explicitly started** by calling `start` on it. This will **start executing the initial
state** of the FSM, which is provided while defining the instance.

@@@ warning {title='Caution'}
Calling `start` more than once is not supported and will lead to unpredictable behaviour. 
@@@

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #start-fsm }

## Wait for Completion

As an FSM has the ability to be complete itself, `await` can be called to **wait for the FSM completion**. **Execution will be paused** at the `await` statement
until the FSM is marked complete.

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #await }

Calling `await` before calling `start` will start the FSM internally and then wait for completion.

## Reactive FSM

Reactive FSM means that changes of state can be tied to changes in Events as well as to Commands.
A FSM can be made to react to changes in Event and Command parameters with help of `Event variables` and `Command flags`.

**`bind`ing FSM to reactive variable is necessary** to achieve the reactive behavior of FSM.

### Event Variables

Event Variables are the way to make an FSM react to CSW Events. An Event Variable can be tied to only one Parameter Key in an event.
To make the FSM react to Event variable, we need to create an `EventVariable` for a specific Parameter Key of an Event and **bind the FSM** to it.
FSM can be bind to multiple Event variables and vise versa.

Event Variables use the CSW Event Service underneath. This could be used to share data between multiple sequencers.
Whenever any event is published on the key of given event, all the FSMs bound to that variable will be re-evaluated.

Event Variables are of 2 types:

1. `SystemVar` - are based on SystemEvent
2. `ObserveVar` - are based on ObserveEvent

Event variable takes 4 arguments: 
- the initial value to set in Event parameter against the given parameter Key
- the event key to tie the Event variable to
- the param Key whose value to read from Event parameters
- the duration (optional) of the polling (Significance of duration parameter is explained @ref:[below](#poll).)

Event Variable has the ability to behave in one of two ways:

- Subscribe to the Events getting published
- Poll for a new event with a specified period

#### Subscribe to an Event

Event Variable subscribes to the given Event key and re-evaluates the FSMs current state each time an event is published.

The following example shows how to create Event Variables with the subscribing behavior, `bind` FSM to it and methods like `get` and `set`.
`set` will publish the event with modified parameter.

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #event-var }

#### Poll

Polling behavior is for situations when it's **not necessary to re-evaluate FSM state on every Event** and can be done periodically.
An Event Variable can poll to get the latest Event value with given period and if a new Event is published, it will re-evaluate the FSMs current state.
Polling behavior can be used when the publisher is too fast and there is no need respond so quickly to it.

To create an Event Variable with polling behavior, it needs an extra argument which is the `duration` to poll with. The example code demos
this feature.
Other methods like `get`, `set` and `bind` are same as shown @ref:[Subscribe](#subscribe-to-an-event) examples above.

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #polling }

### CommandFlag

Command Flag acts as bridge that can used to pass `Parameters` to an FSM from outside. Setting the parameters in a Command Flag will re-evaluate
all the FSMs with provided params that are bound to that flag. It is possible to bind one FSM to multiple Command Flags and vise versa.
Command Flag is limited to scope of a single script. It does not have any remote impact.

The following example shows how to create a `CommandFlag`, `bind` FSM to it, and use methods `get` and `set` which are provided to retrieve or set the value of
parameters in the Command Flag.

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #command-flag }

@@@ note
- Binding FSM to reactive variables can be done anytime in the lifecycle of FSM not only before starting it. 
Doing it after completion of FSM does not do anything.
- **Binding is necessary to achieve the reactive behavior**.
@@@

## Example FSM

In the below example, `temparatureFsm` demonstrates how to define and use FSM in the scripts. The Event Variable is declared
with an event key `esw.temperature.temp` for parameter `temperature` and `temperatureFsm` is bound to it. The job of the `temperatureFsm`
is to decide the `state` based on the `temperature` and publish it on event key `esw.temperatureFsm` with param key `state`.

Logic of state change is:

| condition |state |
| :---: | :---: |
|  temp == 30 |  FINISH |
|  temp > 40  |  ERROR  |
|  else       |  OK     |

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/FsmExample.kts) { #example-fsm }

Full example code is available [here]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/FsmExample.kts).

Key things in above example code are :

- `[[ 1 ]]`: Shows **top-level scope of the FSM which can used to declare variables** in FSM's scope and statements which should be executed while starting the FSM.
Statements written here will be executed only once when the FSM starts.
- `[[ 2 ]]`: The scope of the state. Statements written here will be executed on every evaluation of the state. So variables declared here will be reinitialized
whenever state is re-evaluated. In the above case, the *expectedTemp* and *currentTemp* will be initialized every time the OK state is evaluated.
- `[[ 3 ]]`: State transitions from `OK` state to `FINISHED`.
- `[[ 4 ]]`: State transitions from `OK` state to `ERROR` with *Params*. ERROR state shows how to consume Params in a state.
- `[[ 5 ]]`: Marks the FSM complete. Re-evaluation or state transitions cannot happen after this is executed.

Till point `[[ 5 ]]`, it's all about **defining the blue-print** and **initialising state of FSM** which includes executing statements at `[[ 1 ]]`.

- `[[ 6 ]]`: Shows the binding `temperatureFsm` to `temperatureVar` and `commandFlag`. After this point, FSM will re-evaluate whenever events are published on `temperatureVar`.
- `[[ 7 ]]`: Starts **evaluating the initial state** of the FSM
- `[[ 8 ]]`: Sets the Params of the Command in the Command flag
- `[[ 9 ]]`: Waits for completion of the FSM. In example, the script execution will be blocked till line `[[ 4 ]]` is executed which will mark the FSM complete. The script will
continue execution after FSM is marked complete.

Example code also demos the use of the @ref:[helper constructs](#fsm-helper-constructs) like `entry`, `on`.
