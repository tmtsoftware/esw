# Sequencer Technical Design
## Introduction

Sequencer is OMOA component which has responsibility of executing Sequence of Steps. In an observation, Sequencers will form
hierarchy where top-level Sequencer sending sequence to downstream sequencers and downstream Sequencers sending commands to Assemblies/HCDs.
Sequencer has two main parts:

1. Sequencer Framework
2. Scripting Support

Sequencer Framework uses Akka Actor at a core and is responsible for executing Sequence, calling handlers from Script.
Sequencer Script defines behaviour of Sequencer while executing Sequence. Scripts are written using Domain Specific Language
provided as a part of Framework.

## Modules

* esw-ocs-api -
This is cross module, which is compiled into JVM as well as JS code. This consists of `SequencerApi`
which defines an interface for Sequencer. This module also consists of core models, actor client, JVM and JS client for
Sequencer.

* esw-ocs-impl -
This module consists of core implementation of Sequencer which is `SequencerBehaviour` (Sequencer Actor), Engine and SequencerData.

* esw-ocs-app -
This module consists of wiring as well as cli application to start Sequencer.

* esw-ocs-dsl -
This module consists of Scala counterpart of Script DSL.

* esw-ocs-dsl-kt -
This module consists of Kotlin counterpart of Script DSL.

* esw-ocs-handler -
This sequence manager handler module is responsible for providing HTTP routes for Sequencer HTTP server.

## Sequencer Interfaces

Sequencer exposes two interfaces to outer world.

1. Akka interface - Sequencer is registered as Akka registration. One can resolve Sequencer and use Akka client to interact with Sequencer.
2. HTTP interface - Sequencer also exposes HTTP inteface as an embedded Sequencer Server (internal usage) as well as one can interact with Sequencer using
gateway (as public interface). Please refer Gateway documentation for details.

## Implementation Details

Sequencer framework uses Akka Actor as core implementation (Sequencer Actor).
Following figure explains architecture of Sequencer framework. Sequencer is registered with Location Service. SOSS will resolve
Sequencer location and will send Sequence to top-level Sequencer. Engine continuously poll for next step as soon as previous step
is finished with success. It will execute step using appropriate handler written in Script.
If any step in Sequence fails, Sequence is terminated with Error. Engine and Sequencer Actor are core parts of Framework. Framework
part is same for every Sequencer and Script is a variable part. Script defines behaviour of Sequencer for each step within Sequence.

![Sequencer Architecture](../images/ocs/sequencer.png)

Following sections explains core components of Sequencer:

1. Sequencer Lifecycle
2. Scripting Support

### Sequencer Lifecycle

The Sequencer lifecycle is implemented as a Finite State Machine. This Section explains different states and messges accepted in
respective state of Sequencer. At any given time a Sequencer is in exactly one of those states. The state of the Sequencer is
tied to whether or not it has received a Sequence and whether or not the Sequence has started executing.
Sequencer supports a set of commands/messages, and on receiving those commands, it takes an action and transitions to other states.

Following are the states supported by the Sequencer:

* **Idle/Online:** This is the default state of the Sequencer. A Sequencer is idle when it is starts up. It has a Script since it
has been loaded by the Sequence Component, but there's no Sequence under execution.
A Sequencer can come to the idle state from the following situations:

    * when the Sequencer starts up for the first time with a Script loaded
    * when the Sequencer has finished execution of a Sequence
    * when the Sequencer was offline, and a goOnline command is sent

In this state, the Sequencer can only receive a Sequence, `goOffline`, or `shutdown`, in which the Sequencer transitions to the
`Loaded`, `Offline`, and `Killed` states, respectively.

* **Loaded:** A Sequencer is in loaded state when a Sequence is received and ready for execution, but execution of the Sequence hasn't started.
A separate `start` command is expected to start execution of the Sequence.
All sequence editor actions (for e.g. add, remove, reset) are accepted in this state.
From this state, the Sequencer can go to the `InProgress` state
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

Sequencer Scripts are most important part of Sequencer Architecture. Scripting environment has following core requirements:

1. Domain Specific Language (DSL) constructs for writing Scripts. For example, `par` to execute commands in parallel, `onSetup` like
constructs where script writer will define logic to be executed when `Setup` command is received. Kotlin is used provide DSL for writing Script. Kotlin has excellent
language support for writing embedded DSL.

2. Mutable State within the script. To handle mutable state in thread safe manner, Script is implemented using Active Object Pattern.
Every operation in Script need to be asynchronous and non-blocking in nature and this operation will be scheduled on Single Threaded Execution Context (StrandEC).
This ensures that state inside Script can be accessed/modified at any place inside with the guaranty of thread safety. If there is need to have
CPU intensive or blocking operations in Script, pattern supporting these needs to be followed which uses another Execution Context so that Script StrandEC is not blocked.


## Running Sequencer

For running Sequencer Script, please refer @ref:[this](../sequencersandscripts/sequencer-app.md).
