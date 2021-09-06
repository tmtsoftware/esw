# ESW Shell - A Testing Environment for ESW and CSW

This project contains an interactive shell powered by [Ammonite](https://ammonite.io/#Ammonite-REPL) that 
provides access to all the major CSW and ESW services via a command-line interface (CLI).
ESW-shell can be used to communicate with Assemblies, HCDs (Hardware Control Daemon), and Sequencers using TMT Common Software ([CSW](https://github.com/tmtsoftware/csw)) APIs. 

Additionally, there are ESW-provided Sequencer commands and ESW applications using TMT Executive Software ([ESW](https://github.com/tmtsoftware/esw))
that can be used and communicated with and controlled using ESW-shell.

## 1. Install `coursier` and the TMT Apps Channel

The `coursier` application must be installed on your machine and the OCS Apps channel must be installed.
The instructions for doing this are provided @ref:[here](getting-apps.md).

## 2. Start Any Needed CSW Services or ESW Apps

If you need some CSW Services or Assemblies or HCDs, they should be started depending on your goals
according to steps 3 and 4 of @ref:[here](getting-apps.md).

## 3. Starting esw-shell

Once `coursier` and apps are installed, it can be used to start `esw-shell`, type:

```bash
cs launch esw-shell
```
If you need to start with a specific version, the following can be used:

```bash
cs launch esw-shell:<version | SHA>
```

Normally, you don't need to start the app with a specific version.

## 4. Exiting esw-shell
At any point in time, if you want to exit the shell, type `exit` and press enter.
<br/><br/>

# Using ESW Shell
The following sections provide examples of some built-in capabilities of ESW-shell. 
Note that ESW shell can be extended by the user by defining your own functions and 
saving them in files that can be loaded dynamically whenever needed.

## Imports Available in esw-shell
The contents of the following imports are available in `esw-shell` when it is started. A user does not need to import them again.

Scala
: @@snip [Main.scala](../../../../../esw-shell/src/main/scala/esw/shell/Main.scala) { #imports }

## Usage of Command Service to interact with HCDs, Assemblies and Sequencers
The following is built-in functionality for working with Command Service and Assemblies and HCDs.

### Spawning a Simulated HCD/Assembly
`esw-shell` can be used to spawn a simulated HCD/Assembly using these [Simulated Component Handlers]($github.base_url$/esw-shell/src/main/scala/esw/shell/component/SimulatedComponentHandlers.scala).

`SimulatedComponentHandlers` supports two commands:

- `noop` : This command immediately returns `Completed` response with `runId`
- `sleep` : This command immediately returns `Started` response with `runId` and `Completed` response after some sleep time. This sleep time is specified in `timeInMs` parameter of the command.


The example commands below will spawn a simulated HCD/Assembly without having the need of a running Agent.

```bash
spawnSimulatedHCD("ESW.testHCD1") // "ESW.testHCD1" is the HCD prefix
spawnSimulatedAssembly("ESW.testAssembly") // "ESW.testAssembly" is the assembly prefix
```

### Using predefined component handlers on Agent
The Agent Service can also be used to spawn simulated HCD/Assemblies on any machine that is running an Agent.

These example commands will spawn a simulated HCD/Assembly on `ESW.machine1` Agent. It is assumed that
`ESW.machine1` Agent is already running. For running agent refer @ref:[Agent App](agent-app.md)

```bash
spawnSimulatedHCD("ESW.testHCD", "ESW.machine1") // "ESW.testHCD" is the HCD prefix
spawnSimulatedAssembly("ESW.testAssembly", "ESW.machine1") // "ESW.testAssembly" is the assembly prefix
```

### Using custom component handlers

`esw-shell` can be used to spawn a real HCD/Assembly which uses custom component handlers passed by the user.

The example below will spawn an Assembly that uses the provided component handlers which will be used by the created Assembly.

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
spawnAssemblyWithHandler("ESW.testHCD", componentHandlers) // "ESW.testHCD" is the HCD prefix
spawnHCDWithHandler("ESW.testAssembly", componentHandlers) // "ESW.testAssembly" is the assembly prefix
```

This is a very nice feature!  It can be used to create components that can simulate other components within tests, for instance.

### Finding a required component

To get a handle to a Command Service for a particular HCD/Assembly/Sequencer, use the following `esw-shell` short-cut
commands:

To create a CommandService for an HCD:

- `val hcdComponent = hcdCommandService("IRIS.hcd_name")`

For Assemblies:

- `val assemblyComponent = assemblyCommandService("IRIS.assembly_name")`

This can also be done for a Sequencer in order to send a Sequence to a Sequencer. (See also: @ref:[Sequencers](../sequencer-tech.md))

- `val sequencer = sequencerCommandService(IRIS, "darknight")`

**IRIS.hcd_name** and **IRIS.assembly_name** are the Prefix by which the HCD and Assembly respectively, are registered with Location Service.

**IRIS** and **darknight** are the Subsystem and the observing mode for the Sequencer.

@@@ note
The above calls internally use Location Service to resolve the required HCD/Assembly/Sequencer.
@@@

### Creating a Setup to submit to HCD/Assembly using Command Service
To send a Setup or Observe command to a component, create the Parameters and add them to the Setup or Observe.

```scala
val longKey                     = LongKey.make("timeInMs")
val paramSet: Set[Parameter[_]] = Set(longKey.set(1000))

val setup = Setup(Prefix("iris.filter.wheel"), CommandName("sleep"), Some(ObsId("2020A-001-123")), paramSet)
```

### Creating a Sequence to submit to a Sequencer using Command Service

```scala
val byteKey                     = ByteKey.make("byteKey")
val paramSet: Set[Parameter[_]] = Set(byteKey.set(100, 100))

val setup = Setup(Prefix("iris.filter.wheel"), CommandName("move"), Some(ObsId("2020A-001-123")), paramSet)
val sequence = Sequence(setup)
```
Note that in the last step, the Setup is wrapped in a Sequence to make a Sequence with one Step.

Other than Command Service handles, the following pre-defined handles or factories are available in `esw-shell` to interact with different services:

- For Sequence Manager, use pre-imported `sequenceManager()` handle
- For Agent, create new handle using `agentClient("iris.machine_1")`
- For SequenceComponent, create new handle using `sequenceComponentService("ESW.ESW_1")`
- For AdminApi, use pre-imported `adminApi` handle
- For EventService, use pre-imported `eventService` handle
- For AlarmService, use pre-imported `alarmService` handle

### Creating a CSW ComponentId

```scala
val componentId = ComponentId(Prefix(ESW, "test1"), Assembly)
```

### Creating an Event

Events work much like Setups.

```scala
val byteKey = ByteKey.make("byteKey")
val paramSet: Set[Parameter[_]] = Set(byteKey.set(100, 100 ))
val prefix = Prefix("tcs.assembly")
val event = SystemEvent(prefix, EventName("event_1"), paramSet)
```

### Creating an AlarmKey

```scala
val alarmKey = AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneAxisHighLimitAlarm")
```

### Submitting a Setup to a component

Use the Command Service `submit` call with the Setup created in a previous step to send the Setup to the HCD or Assembly.

The `esw-shell` is just providing a Scala programming environment. Therefore, `submit` returns a response wrapped in a Future, 
you can use `get` to extract the response out of any Future response once it completes. The Future has a default wait 
timeout of `10.seconds` for the future to complete:

- `val hcdResponse = hcdComponent.submit(setup).get`
- `val assemblyResponse = assemblyComponent.submit(setup).get`

If you have a long-running command, you can use `await` method with your own custom timeout:

- `val hcdResponse = hcdComponent.submit(setup).await(20.seconds)`

Submit the Sequence created in a previous step using the Command Service for the Sequencer:

- `val sequencerResponse = sequencer.submit(sequence).get`

The other Command Service calls are also present on a Command Service handle.

### Submitting commands to an ESW service

To Agent:

- `val agent = agentClient("iris.machine_1")`
- `val spawnResponse = agent.spawnSequenceComponent("ESW_1", Some("1.0.0")).get`

To AdminApi:

- `val logMetadata = adminApi.getLogMetadata(componentId).get`

To Event Service:

- `val publishResponse = eventService.publish(event).get`

To AlarmService:

- `val response = alarmService.setSeverity(alarmKey, AlarmSeverity.Major).get`

## Interacting with Sequence Manager

You can also use the Sequence Manager to send commands to the ESW Sequence Manager. Sequence Manager is a service, it is not
an Assembly or HCD. It has its own specialized Sequence Manager API.


A handle to a running @ref:[Sequence Manager](sequence-manager-app.md) can be obtained using:

```bash
val sm = sequenceManager()
```

@@@ note
Unlike Assemblies, and HCDs, there is only one Sequence Manager service at a  time.
@@@

All Sequence Manager APIs can be called upon the handle. For example:

```bash
val configureResponse = sequenceManager.configure(ObsMode("darknight")).get
val shutdownSequencerResponse = sequenceManager.shutdownSequencer(ESW, ObsMode("darknight")).get
val resources = sm.getResources.get
```

## Interacting with Sequence Component

Sequence Components are used to create Sequencers. They, too, can be accessed with `esw-shell`.

Get a handle to a running @ref:[Sequence Component](../../sequencersandscripts/sequencer-app.md) using:

```bash
val sc = sequenceComponentService("ESW.ESW_1")
```

Note that the Sequence Component is registered with a Subsystem.name.


All Sequence Component APIs can be called upon the handle. For example:

```bash
val loadScriptResponse =  sc.loadScript(ESW,ObsMode("darknight")).get
val unloadScriptResponse =  sc.unloadScript().get
val restartScriptResponse =  sc.restartScript().get
```

### Provisioning Sequence Components

In order to provision Sequence Components using Sequence Manager, use some custom version of Sequencer scripts, we have
provided a special method in `esw-shell`:

```bash
provision(ProvisionConfig((Prefix(ESW, "primary") -> 3)), <sequencer scripts version | SHA>)
```

This is useful in case new scripts are to be tested in a dev environment. The Sequencer scripts version for
these new scripts can be provided in this method. It will take care of updating this version in the 
Configuration Service before setting up the Sequence Components.
