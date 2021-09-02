
# ESW Shell

This project contains an interactive REPL shell powered by [Ammonite](https://ammonite.io/#Ammonite-REPL) and allows 
its users to gain access to all the major CSW and ESW services via CLI which then can be used to communicate with an 
HCD (Hardware Control Daemon) and an Assembly using TMT Common Software ([CSW](https://github.com/tmtsoftware/csw)) 
APIs and a Sequencer using TMT Executive Software ([ESW](https://github.com/tmtsoftware/esw)).

## Build Instructions

The build is based on sbt and depends on libraries generated from the
[csw](https://github.com/tmtsoftware/csw) and [esw](https://github.com/tmtsoftware/esw) projects.

## Prerequisites for running Components

CSW Services need to be running before starting the components.
This is done by using the `csw-services.sh` script, which is installed as part of the csw build.
If you are not building CSW from the sources, you can run `csw-services` as follows:

- Install `coursier` using steps described [here](https://tmtsoftware.github.io/csw/apps/csinstallation.html) and add the TMT channel.
- Run `cs install csw-services:<CSW version | SHA>`. This will create an executable file named `csw-services` in the default installation directory.
- Run `csw-services --help` to get more information.
- Run `csw-services start -c` to start the Location Service and Config Service.

## Running the esw-shell using sbt

After making sure that all the pre-requisites are satisfied, we can directly run the esw-shell via sbt from the root directory of the project

- Run `sbt esw-shell/run`

## Running esw-shell using Coursier

Alternatively, you can run esw-shell using Couriser:

- Add the TMT apps channel to your local Coursier installation using the following command:

```bash
cs channel --add https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json
```

- After adding the TMT apps channel, you can simply launch esw-shell by executing:

```bash
cs launch esw-shell:<version | SHA>
```

## Exiting esw-shell

At any point in time, if you want to exit the shell, type `exit` and press enter.

## Imports available in esw-shell

These imports are available in shell, and they do not need to be imported again.  These imports give the user
a wealth of CSW and ESW models to use, as well as providing access to some Java, Scala and Akka classes useful for TMT
component and service interaction.

Scala
: @@snip [Main.scala](../../../../esw-shell/src/main/scala/esw/shell/Main.scala) { #imports }

In addition to these, some factories and convenience methods
are provided and described below.

## Usage of Command Service to interact with HCDs, Assemblies and Sequencers

### Spawning simulated HCD/Assembly

`esw-shell` can be used to spawn a simulated HCD/Assembly which uses the handlers specified in [Simulated Component Handlers]($github.base_url$/esw-shell/src/main/scala/esw/shell/component/SimulatedComponentHandlers.scala)

`SimulatedComponentHandlers` supports two commands.

- `noop` : This command ignores the parameters and immediately returns `Completed` response with `runId`
- `sleep` : This command immediately returns `Started` response with `runId` and `Completed` response after some sleep time. 
This sleep time is specified in a `timeInMs` Long parameter included with the command.

#### Using predefined component handlers

The example commands below will spawn a simulated HCD/Assembly locally.

```bash
spawnSimulatedHCD("ESW.testHCD1") // "ESW.testHCD1" is the HCD prefix
spawnSimulatedAssembly("ESW.testAssembly") // "ESW.testAssembly" is the assembly prefix
```

#### Using predefined component handlers on Agent

The example commands below will spawn a simulated HCD/Assembly on `ESW.machine1` agent. An agent is a remote process
that can be used to spawn components (see @ref:[Agent App](../technical/apps/agent-app.md)).  It is assumed that
`ESW.machine1` is already running. 

```bash
spawnSimulatedHCD("ESW.testHCD", "ESW.machine1") // "ESW.testHCD" is the HCD prefix
spawnSimulatedAssembly("ESW.testAssembly", "ESW.machine1") // "ESW.testAssembly" is the assembly prefix
```

#### Using custom component handlers

`esw-shell` can be used to spawn a real HCD/Assembly which uses custom component handlers passed by the user.

The code below shows an example of this, by spawning an Assembly using `DefaultComponentHandlers` and overriding
its onSubmit method to provide custom command handling functionality.

```bash
val componentHandlers = (ctx, cswCtx) =>
      new DefaultComponentHandlers(ctx, cswCtx) {
        override def onSubmit(runId: Id, controlCommand: ControlCommand): CommandResponse.SubmitResponse = {
          controlCommand.commandName.name match {
            case "sleep" =>
              // do something on receiving move command
              cswCtx.timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusSeconds(5))) {
                cswCtx.commandResponseManager.updateCommand(CommandResponse.Completed(runId))
              }
              CommandResponse.Started(runId)
            case _ => CommandResponse.Completed(runId)
          }
        }
      }
spawnAssemblyWithHandler("ESW.testAssembly", componentHandlers) // "ESW.testAssembly" is the assembly prefix
```

### Sending commands to components

The esw-shell repo provides convenience methods to obtain a Command Service handle for a particular HCD/Assembly
or Sequencer.

For HCDs

- `val hcdComponent = hcdCommandService("iris.hcd_name")`

For Assemblies

- `val assemblyComponent = assemblyCommandService("iris.assembly_name")`

For @ref:[Sequencers](../technical/sequencer-tech.md)

- `val sequencer = sequencerCommandService(IRIS, "darknight")`

**iris.hcd_name** and **iris.assembly_name** are the prefixes by which the HCD and Assembly components were
registered with Location Service respectively.

**IRIS** and **darknight** are the subsystem and the observing mode for the Sequencer respectively.

Note - The above calls internally uses the Location Service to resolve the required component.

### Creating a command to submit to an HCD/Assembly

The following code shows an example of how to create a Setup command.  This syntax is similar to what you would
use in plain Scala.

```scala
val longKey                     = LongKey.make("timeInMs")
val paramSet: Set[Parameter[_]] = Set(longKey.set(1000))

val setup = Setup(Prefix("iris.filter.wheel"), CommandName("sleep"), Some(ObsId("2020A-001-123")), paramSet)
```

### Creating a sequence to submit to a Sequencer

A Sequence is similarly created, except commands must be wrapped in a Sequence object.

```scala
val byteKey                     = ByteKey.make("byteKey")
val paramSet: Set[Parameter[_]] = Set(byteKey.set(100, 100))

val setup = Setup(Prefix("iris.filter.wheel"), CommandName("move"), Some(ObsId("2020A-001-123")), paramSet)
val sequence = Sequence(setup)
```

### Submitting the commands to components

You can submit a Setup command using the Command Service for an HCD/Assembly

The response from a Submit command is wrapped in a Future.  You can use `get` to extract the response out of any Future response.

- `val hcdResponse = hcdComponent.submit(setup).get`
- `val assemblyResponse = assemblyComponent.submit(setup).get`

Submitted commands in the esw-shell have a default wait timeout of `10.seconds` for the future to complete.  
If you have a long running command, you can use the `await` method to specify a custom timeout

- `val hcdResponse = hcdComponent.submit(setup).await(20.seconds)`

Submitting a sequencer to Sequencer is done in a similar way: 

- `val sequencerResponse = sequencer.submit(sequence).get`

### Using the Event Service:

Use of the Event Service is also done similar to normal Scala code.  In most cases you want to use the defaultPublisher
and defaultSubscriber provided by the available eventService reference.  See the [EventService](http://tmtsoftware.github.io/csw/services/event.html) documentation
for more information about usage.

```scala
val byteKey = ByteKey.make("byteKey")
val paramSet: Set[Parameter[_]] = Set(byteKey.set(100, 100 ))
val prefix = Prefix("tcs.assembly")
val event = SystemEvent(prefix, EventName("event_1"), paramSet)

val sub = eventService.defaultSubscriber.subscribeCallback(Set(EventKey(prefix, EventName("event_1"))), 
  event=>println("got event"))
val publishResponse = eventService.defaultPublisher.publish(event).get
```

### Using other Services

Other than Command Service handles, the following pre-defined handles or factories are available in the shell to interact
with different services:

- For the Sequence Manager, use the pre-imported `sequenceManager()` handle
- For an Agent, create a new handle using `agentClient("iris.machine_1")`
- For a SequenceComponent, create a new handle using `sequenceComponentService("ESW.ESW_1")`
- For an AdminApi, use the pre-imported `adminApi` handle
- For the EventService, use the pre-imported `eventService` handle
- For the AlarmService, use the pre-imported `alarmService` handle

After obtaining a reference to a CSW or ESW service, client methods can be used as they would in normal Scala code.

Agent:

- `val agent = agentClient("iris.machine_1")`
- `val spawnResponse = agent.spawnSequenceComponent("ESW_1", Some("1.0.0")).get`

AdminApi:

- `val componentId = ComponentId(Prefix(ESW, "test1"), Assembly)`
- `val logMetadata = adminApi.getLogMetadata(componentId).get`

AlarmService:

- `val alarmKey = AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneAxisHighLimitAlarm")`
- `val response = alarmService.setSeverity(alarmKey, AlarmSeverity.Major).get`

## Interacting with the Sequence Manager

Handler to running @ref:[Sequence Manager](../technical/apps/sequence-manager-app.md) can be obtained using:

```bash
val sm = sequenceManager()
```

All Sequence Manager APIs can be called upon the handle. For example:

```bash
val configureResponse = sequenceManager.configure(ObsMode("darknight")).get
val shutdownSequencerResponse = sequenceManager.shutdownSequencer(ESW, ObsMode("darknight")).get
val resources = sm.getResources.get
```

## Interacting with a Sequence Component

A handler to a running @ref:[Sequence Component](../sequencersandscripts/sequencer-app.md) can be obtained using:

```bash
val sc = sequenceComponentService("ESW.ESW_1")
```

All Sequence Component APIs can be called using the handle. For example:

```bash
val loadScriptResponse =  sc.loadScript(ESW,ObsMode("darknight")).get
val unloadScriptResponse =  sc.unloadScript().get
val restartScriptResponse =  sc.restartScript().get
```

### Provisioning sequence components

In order to provision sequence components that use some custom version of the sequencer scripts, we have
provided special method in `esw-shell`:

```bash
provision(ProvisionConfig((Prefix(ESW, "primary") -> 3)), <sequencer scripts version | SHA>)
```

This is useful in case new scripts are to be tested out in a development environment. The sequencer scripts version for
these new scripts can be provided in this method. It will take care of updating this version in the config service 
before setting up the sequence components.
