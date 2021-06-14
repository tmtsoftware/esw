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

## Sequence execution process

#### Starting a Sequencer
When we do load script from a sequence component, it creates a [Sequencer Wiring]($github.base_url$/esw-ocs/esw-ocs-app/src/main/scala/esw/ocs/app/wiring/SequencerWiring.scala). 
Sequencer Wiring passes the Kotlin script class name as string parameter to [Script Loader]($github.base_url$/esw-ocs/esw-ocs-impl/src/main/scala/esw/ocs/impl/script/ScriptLoader.scala), 
which uses Java reflection APIs to dynamically load script class with given name and create its instance. This loaded script is then passed to
an execution [Engine]($github.base_url$/esw-ocs/esw-ocs-impl/src/main/scala/esw/ocs/impl/core/Engine.scala) which is responsible for processing each step.
After initialization, Sequencer's Akka and HTTP connection is registered to Location Service.

#### Loading and Running a sequence in Sequencer

* During initialization of Sequencer, it is set to IDLE state and initialized with empty data in Sequence Data like empty Step List,etc.
* Once initialized, user can either **Load and start a sequence** or **Submit a sequence**, in both cases list of commands will be converted to richer model of list of Steps.
    * **Load and Start a Sequence**: Using `loadSequence` api, `SequencerData`(described below) will be initialized with Sequence Steps.
      User can then use `startSequence` api to start the execution of steps.
    * **Submit a Sequence**: Using `submit` / `submitAndWait` api, `SequencerData` will be initialized with Sequence Steps, and start the sequence execution.

    After any of above flow, Sequencer will go into RUNNING state.

`SequencerData` has different fields as follows:

* `stepList` - This will store steps of the Sequence
* `runId` - This is runId of Sequence
* `sequenceResponseSubscribers` - This is a list of Subscribers who is interested in response either by **submitting a sequence** or **querying response**.
    * When subscriber has submitted the sequence using `submitAndWait` API, it will get response once Sequence is completed with Success/Failure
    * For other Subscribers(who has not submitted the sequence), they can also get same response using `queryFinal` API, for this they need to provide runId of Sequence

##### Mapping between steps and script handlers

A Kotlin script file contains multiple command names and associated handler blocks with them.
```
onSetup("commandName") { 
  ...handler block of command
}
```
When a Script is loaded, [ScriptDsl]($github.base_url$/esw-ocs/esw-ocs-dsl/src/main/scala/esw/ocs/dsl/script/ScriptDsl.scala) stores all command names with respective handler code blocks, 
you can think of it like a map with command name as key and handler code block as its value.

The sequence submitted by a client to sequencer contains list of commands that need to be executed. These commands are stored as richer model of steps.
When a step is picked for execution by Engine, its corresponding code block is picked from mapping in ScriptDsl and that block is executed.

@@@note
Engine is a continuous running loop, it pulls next step once current step is finished.
@@@

#### Completion of a Sequence
Once every step is executed, it is marked as Finished with Success or Failure.
If any of step is Failed, Sequence is terminated and Error response is sent to all Subscribers.
If all steps are completed with Success, then Success response is sent to all Subscribers.


## Implementation Details

Sequencer framework uses Akka Actor as core implementation (Sequencer Actor).
The following figure explains the architecture of the Sequencer framework. Sequencer is registered with Location Service. The future
SOSS Planning Tool or ESW.HCMS Script Monitoring Tool will use the Location of the top-level Sequencer returned by Sequence Manager
to resolve the top-level Sequencer, and will send the Observation's Sequence to top-level Sequencer.

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
tied to whether or not it has received a Sequence and whether the Sequence has started executing.
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

Scripting Support is implemented using Kotlin. `onSetup` and `onObserve` handlers are provided which will be used by script writers to write behaviour when `Setup`
and `Observe` commands are received. Specific `onSetup` handler will be picked based on command name specified in handler. For more details
about scripting please refer @ref:[here](../scripts/scripts-index.md)

## Sequencer Interfaces

Sequencer exposes its interface in three ways:

1. Akka interface - Sequencer is registered as an Akka-based component. One can resolve Sequencer and use the Akka client to interact with Sequencer.
2. HTTP direct interface - Each Sequencer also exposes an HTTP-based interface as an embedded Sequencer
Server (direct and unprotected usage). This access provides routes that allow user to directly control the Sequencer without any auth protection.
UI applications are supposed to use Gateway interface described below to interact with Sequencer as Gateway provided auth protection layer.
3. HTTP Gateway interface - It is also possible to interact with Sequencer using the UI Application Gateway (as outside network interface).
Being outside network interface, this access requires user to be authenticated and authorized. The Gateway hosts the Sequencer API,
which communicates with the Sequencer via the Akka interface. Please refer to the Gateway documentation for @ref[more information](../uisupport/gateway.md).

Following snippet shows instantiating Akka Interface to interact with Sequencer:

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #instantiate-akka-interface }

Following snippet shows instantiating HTTP direct Interface to interact with Sequencer:

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #instantiate-http-direct-interface }

For interacting using HTTP Gateway interface, please refer @ref[here](./gateway-tech.md)

@@@note
Sequencer Http direct interface is not supposed to be used from anywhere as it is unprotected and also bind to inside network ip.
@@@

## Interacting with Sequencer

One can use Akka Interface or HTTP Gateway interface to interact with Sequencer. APIs to interact with Sequencer are
broadly categorised as following.

* Sequencer Command Service - Provided as a part of CSW. Provides way to submit sequence and receive response.
* Sequence Editor APIs - Provided as a part of ESW. Provided way to edit sequence submitted to Sequencer.
* Sequencer Lifecycle APIs - Provided as a part of ESW. Provided way to send lifecycle commands to Sequencer.
* Other APIs - Provided as a part of ESW.

### Sequencer Command Service

Commands can be sent to Sequencer to submit sequence and response is received in return.

@ref:[Sequencer Interface](#sequencer-interfaces) exposes APIs on top of @extref[Sequencer Command Service](csw:services/sequencer-command-service). Sequencer Command
Service provides way to submit sequence to Sequencer and receive started or final response. Sequencer Command Service is provided as
a part of CSW and details about using Sequencer Command Service can be found @extref[here](csw:services/sequencer-command-service).

### Sequence Editor APIs

Sequence Editor APIs allow actions to edit sequence such as add more steps, delete/replace existing steps, Add/remove breakpoint
in sequence. For using Sequence Editor actions, sequencer must be running a sequence. If Sequencer is not running any sequence then,
Sequencer will return `Unhandled` response.

* add

This API allows to add more steps to sequence. Steps will be added in the end of sequence.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #add }

* prepend

This API allows to add more steps to sequence. Steps will be added after currently running step of sequence.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #prepend }

* getSequence

This API allows returns Sequence running in Sequencer if any.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #getSequence }

* replace

This API allows to replace particular step in the sequence with more steps.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #replace }

* insertAfter

This API allows to insert more steps after particular step in the sequence.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #insertAfter }

* delete

This API allows to delete particular step in the sequence.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #delete }

* add and remove breakpoint

These APIs allows to add and remove breakpoint for particular step in the sequence.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #addRemoveBreakpoint }

* reset

These APIs allows to discard all pending steps in the sequence.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #reset }

* pause and resume sequence

These APIs allows to pause and resume sequence. This essentially adds/removes breakpoint at first pending step in sequence

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #pause-resume }

### Sequencer Lifecycle APIs

Sequencer Lifecycle APIs allow to send lifecycle commands to Sequencer such as goOnline, abortSequence etc.

Certain commands are restricted depending on state of Sequencer. For example, goOnline command is handled only when Sequencer is
in Offline state. If goOnline is sent otherwise it will return `Unhandled` response with error msg.
For details refer @ref:[Sequencer Lifecycle Section](#sequencer-lifecycle)

* isAvailable

This API allows to check if Sequencer is in Idle state or not. It returns true if Sequencer is in Idle state.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #isAvailable }

* online/offline

These APIs allow to send goOnline/goOffline mode commands to Sequencer. `isOnline` command returns true if Sequencer is online.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #online-offline }

* abortSequence

This API allow to abort running sequence. This essentially discards pending steps from sequence and also call `onAbortSequence` handler
written in script.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #abortSequence }

* Stop

This API allow to stop sequence. This essentially discards pending steps from sequence and also call `onStop` handler
written in script.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #stop }

* getSequencerState

Sequencer is implememted as state machine. It accepts/discards msgs based on Sequencer State. This API allow returns current sequencer state.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #getSequencerState }

* diagnosticMode

This API allow to send diagnosticMode command to Sequencer. This calls `onDiagnosticMode` handler written in script

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #diagnosticMode }

* operationsMode

This API allow to send operationsMode command to Sequencer. This calls `onOperationsMode` handler written in script

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #operationsMode }

* subscribeSequencerState

This API allows subscribing to state of Sequencer. It returns a Source of `SequencerStateResponse` which contains current `SequencerState` and `StepList`.
The `Subscription` can be used to unsubscribe.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #subscribeSequencerState }

### Other APIs

* loadSequence

This API allows to load sequence in Sequencer. Loaded Sequence does not start execution unless `StartSequence` Command is received.
One can replace already loaded sequence by firing another `loadSequence` command.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #loadSequence }

* startSequence

This API allows to start execution of previously loaded sequence in Sequencer. This return `SubmitResponse` which is `Started` in case
of success.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #startSequence }

* getSequenceComponent

This API allows to get location of Sequence Component running the Sequencer.

Scala
: @@snip [SequencerAPIExample.scala](../../../../examples/src/main/scala/esw/examples/SequencerAPIExample.scala) { #getSequenceComponent }

## Running Sequencer

For running Sequencer Script, please refer @ref:[this](../sequencersandscripts/sequencer-app.md).
