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
and start executing it**. An `InvalidStateException` will be thrown if the provided next state is not defined.

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #state-transition }


@@@ warning {title='Caution with Become'}
State transition should ideally be the **last call in state** or should be **done with proper control flow** so that become is **not called multiple times**.
@@@

Along with changing state, it is also possible to pass *Params* from the current state to the next state. Params can be given to *become* as the last argument, which will 
then be injected in the next state as a parameter.

In the case where **state transition does not happen** while executing a state, the **FSM will stay in the same state** and any re-evaluation of the FSM after that will execute
the same state until a state transition happens. The @ref:[reactive variables](#reactive-fsm) plays an important role in this as they are the way to
re-evaluate the FSM state.

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #state-transition-on-re-evaluation }

In the example above, the FSM is in LOW state. If the temperature is below 20, then there won't be any state transition, 
and the FSM in remain in the LOW state. A change in temperature after that will re-evaluate the "LOW" state again and if 
the temperature is greater than or equal to 20, then current state will change to HIGH. In the example `temperature` is 
an @ref:[event based variable](#event-based-variables) which enables re-evaluation of the current state on changes in temperature value.

### Complete FSM

`completeFsm` **marks the FSM as complete**. Calling it will immediately **stop execution of the FSM** and next steps 
will be ignored. Therefore, it should be called at the end of a state.

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

Reactive FSM means that changes of state can be tied to changes in Events as well as Commands.
An FSM can be made to react to changes in Event and Command parameters with the help of `Event based variables` and `Command flags`.
It is necessary to _bind_ an FSM to reactive variables to achieve the reactive behavior.

### Event-based variables

Event-based variables are the way to make an FSM react to CSW Events.  They are linked to Events (or Parameters of Events) 
and are then bound to an FSM such that when the value of the linked Event (or Parameter) changes, the FSM is re-evaluated.
Event-based variables can be used to share data between multiple sequencers using Events.
 
There are two types of Event-based variables.

#### EventVariable
An `EventVariable` will be tied to an Event published on the given EventKey. 
The example below shows creating an instance of an EventVariable and the *getEvent* method which returns the latest event. 

An EventVariable needs 2 parameters:

- *event key*: specifies which Event to tie the variable to
- *duration*: (optional) polling period for updating the value of the Event (Significance of duration parameter is explained @ref:[below](#poll).)

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #event-var }

#### ParamVariable
A `ParamVariable` will be tied to a specific Parameter Key of an Event published on given EventKey
The example below shows creating an instance of a ParamVariable and the usage of other helper methods. 

A ParamVariable takes 4 parameters:

- *initial*: initial value for the Parameter. The value of the parameter in the Event is updated when the ParamVariable is created.
- *event key*:  specifies the Event with the linked Parameter
- *param Key*: specifies which Parameter to tie the variable to
- *duration*: (optional) polling period for updating the value of the Parameter (Significance of duration parameter is explained @ref:[below](#poll).)

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #param-var }


To make the FSM react to Event-based variables, we need to create an instance of the above event based variables and **bind the FSM** to it.

An FSM can be bound to multiple variables and vice versa.

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #binding }


Event-based variables have the **ability to behave in one of two ways**:

- Subscribe to the Events getting published
- Poll for a new event with a specified period

#### Subscribe to an Event

If the `duration` parameter of an Event-based variable is **not** specified, a subscription is made to the Event and the value 
is updated (and the current state of the FSM is re-evaluated) whenever it is published. 

The following example shows how to create Event Variables with the subscribing behavior and `bind` FSM to it.

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #subscribing }

#### Poll

If it is preferable to have the FSM re-evaluated at a constant periodic rate regardless of when new Events are published, 
polling behavior can be used by specifying the `duration` parameter when creating the Event-based variable. 
This can be useful when the publisher is too fast and there is no need respond so quickly to it.

The example code demos this feature. The *bind*ing part is same as in previous example. 

Kotlin
:   @@snip [Fsm.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/Fsm.kts) { #polling }

### CommandFlag

Command Flag acts as bridge that can be used to pass `Parameters` to an FSM from outside. Setting the parameters in a Command Flag will re-evaluate
all the FSMs with the params that are bound to that flag. It is possible to bind one FSM to multiple Command Flags and vice versa.
A Command Flag is limited to the scope of a single script. It does not have any remote impact.

The following example shows how to create a `CommandFlag`, `bind` an FSM to it, and use the methods `get` and `set`, which are provided to retrieve or set the value of
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
with the Event key `esw.temperature.temp` and parameter `temperature`, and the `temperatureFsm` is bound to it. The job 
of the `temperatureFsm` is to decide the `state` based on the `temperature` and publish it on the EventKey `esw.temperatureFsm` 
with the ParamKey `state`.

Logic of state change is:

| condition |state |
| :---: | :---: |
|  temp == 30 |  FINISH |
|  temp > expectedTemp  |  ERROR  |
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
