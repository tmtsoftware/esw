# Sequencer Technical Documentation

Sequencer is OMOA component which has responsibility of executing Sequence of Steps. In an observation, Sequencers will form
a hierarchy where with a top-level ESW Sequencer sending Sequences to downstream Sequencers and downstream Sequencers sending commands to Assemblies/HCDs.

The Sequencer implementation has two main parts:

1. Sequencer Framework
2. Scripting Support

Sequencer Framework uses an Akka Actor at a core and is responsible for executing the received Sequence and calling handlers in the Script.
Sequencer Scripting Support defines behaviour of Sequencer while executing Sequence. Scripts are written using Domain Specific Language
provided as a part of Framework.

## Modules

* esw-ocs-api -
This is cross-compiled module, which is compiled into JVM as well as JavaScript code. This module includes `SequencerApi`
which defines an interface for Sequencer. This module also consists of core models, actor client, JVM and JavaScript client for
Sequencer.

* esw-ocs-impl -
This module consists of the core implementation of Sequencer the actor which is `SequencerBehaviour` (Sequencer Actor),
Engine and SequencerData.

* esw-ocs-app -
This module consists of wiring as well as cli application to start Sequencer. The wiring integrates Sequencer into
the rest of the ESW/CSW environment.

* esw-ocs-dsl -
This module consists of Scala implementation supporting the Script DSL.

* esw-ocs-dsl-kt -
This module consists of Kotlin counterpart of the Script DSL.

* esw-ocs-handler -
This handler module is responsible for providing HTTP routes for Sequencer HTTP server. Sequencer provides
an HTTP and Akka interface. The HTTP routes are defined and implemented here.

## Sequencer Interfaces

Sequencer exposes its interface in three ways:

1. Akka interface - Sequencer is registered as an Akka-based component. One can resolve Sequencer and use the Akka client to interact with Sequencer.
2. HTTP direct interface - Each Sequencer also exposes an HTTP-based interface as an embedded Sequencer
Server (direct and unprotected usage). This access provides routes that allow user to directly control the Sequencer without any auth protection.
UI applications are supposed to use Gateway interface described below to interact with Sequencer as Gateway provided auth protection layer.
3. HTTP Gateway interface - It is also possible to interact with Sequencer using the UI Application Gateway (as outside network interface).
Being outside network interface, this access requires user to be authenticated and authorized. The Gateway hosts the Sequencer API,
which communicates with the Sequencer via the Akka interface. Please refer to the Gateway documentation for @ref[more information](../uisupport/gateway.md).

## Implementation Details

Sequencer framework uses Akka Actor as core implementation (Sequencer Actor).
The following figure explains the architecture of the Sequencer framework. Sequencer is registered with Location Service. The future
SOSS Planning Tool or ESW.HCMS Script Monitoring Tool will use the Location of the top-level Sequencer returned by Sequence Manager
to resolve the top-level Sequencer, and will send the Observation's Sequence to top-level Sequencer.
Once the Sequence is started, Engine continuously polls for a next step as soon as the previous step is finished with success.
It will execute the step using the appropriate handler written in the Script.
If any step in Sequence fails, the Sequence is terminated with Error, and an Error is returned to the caller. The
submission of a Sequence to the Sequencer uses CSW Command Service (i.e. submit, submitAndWait, etc.).

Engine and Sequencer Actor are core parts of Framework. The framework part is the same for every Sequencer,
but the Script can vary. The Script defines the behaviour of the Sequencer for each step within a Sequence.

![Sequencer Architecture](../images/ocs/sequencer.png)

The following sections explain the core components of Sequencer:

1. Sequencer Lifecycle
2. Scripting Support

### Sequencer Lifecycle

The Sequencer lifecycle is implemented as a fairly complicated finite state machine as shown in the figure below.
This Section explains the different states and messages accepted in each respective state of Sequencer.
At any given time a Sequencer is in exactly one of these states. The state of the Sequencer is
tied to whether or not it has received a Sequence and whether or not the Sequence has started executing.
Sequencer supports a set of commands/messages, and on receiving those commands, it takes an action and transitions to other states.

Following are the states supported by the Sequencer:

* **Idle/Online:** This is the default state of the Sequencer. A Sequencer is idle when it is starts up. It has a Script since it
has been loaded/created by the Sequence Component, but there is no Sequence under execution.
A Sequencer can come to the idle state from the following situations:

    * when the Sequencer starts up for the first time with a Script loaded
    * when the Sequencer has finished execution of a Sequence
    * when the Sequencer was offline, and a goOnline command is received

In this state, the Sequencer can only receive a Sequence, `goOffline`, or `shutdown`, in which the Sequencer transitions to the
`Loaded`, `Offline`, and `Killed` states, respectively.

* **Loaded:** A Sequencer is in loaded state when a Sequence is received and ready for execution, but execution of the Sequence hasn't started.
A separate `start` command is expected to start execution of the Sequence.
All sequence editor actions (for e.g. add, remove, reset) are accepted in this state.
From this state, the Sequencer can go to the `Running` state
on receiving a `start` command, or it could go to the `Offline` state if `goOffline` command is sent. On receiving a `reset` command,
which discards all the pending steps, the Sequencer will go to `Idle` state.

* **InProgress/Running:** The Sequencer is in the `Running` state only when it is executing a Sequence. All sequence editor actions
(for e.g. add, remove, reset) are accepted in this state. From the `Running` state, the Sequencer can go to `Idle` state on completion of a Sequence,
or it can be `shutdown`. A Sequencer cannot go `Offline` from this state; the Sequencer must first to go to the `Idle` state and then `Offline`.

* **Offline:** The Sequencer goes to the `Offline` state only on receiving a `goOffline` command, which can either come from an upstream
Sequencer, or from a user through the admin dashboard. In this state, only a few commands are excepted (for eg. goOnline, shutdown, status etc).

* **Killed:** This is the final state of the Sequencer, achieved when receiving a `shutdown` command. The shutdown command
can be sent in any state, hence a Sequencer can transition to this state from any other state.  However, a Sequencer doesn't
stay in this state long; when a Sequencer is killed, it is removed from the Location Service, and ActorSystem is shutdown,
effectively destroying the Sequencer.


![sequencer-state-transition](../images/ocs/state-transition.png)


### Scripting Support

Sequencer Scripts are the most important part of the Sequencer architecture. The scripting environment has following core requirements:

**Domain Specific Language** (DSL) constructs for writing Scripts. For example, `par` to execute commands in parallel, `onSetup` like
constructs where the script writer will define logic to be executed when `Setup` steps are processed.
Kotlin has been used to create the DSL for writing a Script. Kotlin has excellent language support for writing an embedded DSL.
Kotlin also has excellent support for asynchronous processing/tasks that allows a more script-like syntax for the kinds of things
ESW Scripts need to do.

**Mutable State within the script** To handle mutable state in thread safe manner, Script is implemented using the Active Object Pattern.
Every operation in a Script needs to be asynchronous and non-blocking in nature, and each operation will be scheduled on Single Threaded Execution Context (StrandEC).
This ensures that state inside the Script can be accessed/modified at any place inside with the guarantee of thread safety. If there is need to have
CPU intensive or blocking operations in Script, patterns supporting these needs to be followed which uses another Execution Context so that Script StrandEC is not blocked.
The scripting DSL provides special constructs for background processing.


## Running Sequencer

For running Sequencer Script, please refer @ref:[this](../sequencersandscripts/sequencer-app.md).
