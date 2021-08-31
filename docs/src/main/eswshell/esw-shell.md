# ESW Shell

This project contains an interactive shell and allows its users to gain access to all the major CSW and ESW services via CLI
which then can be used to communicate with a HCD (Hardware Control Daemon) and an Assembly using
TMT Common Software ([CSW](https://github.com/tmtsoftware/csw)) APIs and with a Sequencer using
TMT Executive Software ([ESW](https://github.com/tmtsoftware/esw)).

## Build Instructions

The build is based on sbt and depends on libraries generated from the
[csw](https://github.com/tmtsoftware/csw) and [esw](https://github.com/tmtsoftware/esw) project.

## Prerequisites for running Components

The CSW services need to be running before starting the components.
This is done by starting the `csw-services.sh` script, which is installed as part of the csw build.
If you are not building csw from the sources, you can run `csw-services` as follows:

- Install `coursier` using steps described [here](https://tmtsoftware.github.io/csw/apps/csinstallation.html) and add TMT channel.
- Run `cs install csw-services:<CSW version | SHA>`. This will create an executable file named `csw-services` in the default installation directory.
- Run `csw-services --help` to get more information.
- Run `csw-services start -c` to start the location service and config server.

## Running the esw-shell using sbt

After making sure that all the pre-requisites are satisfied, we can directly run the esw-shell via sbt from the root directory of the project

- Run `sbt esw-shell/run`

## Running esw-shell using Coursier

- Add TMT apps channel to your local Coursier installation using below command

```bash
cs channel --add https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json
```

- After adding TMT apps channel you can simply launch esw-shell by executing

```bash
cs launch esw-shell:<version | SHA>
```

## Usage of Command Service to interact with HCDs, Assemblies and Sequencers

### Spawning simulated HCD/Assembly

`esw-shell` can be used to spawn simulated HCD/Assembly which uses the handlers specified in [Simulated Component Handlers]($github.base_url$/esw-shell/src/main/scala/esw/shell/component/SimulatedComponentHandlers.scala)

Below example commands will spawn a simulated HCD/Assembly on `ESW.machine1` agent. It is assumed that `ESW.machine1` is already running.

```bash
spawnSimulatedHCD("ESW.testHCD", "ESW.machine1") // "ESW.testHCD" is the HCD prefix
spawnSimulatedAssembly("ESW.testAssembly", "ESW.machine1") // "ESW.testAssembly" is the assembly prefix
```

### Finding the required component

Get handle to the command service for a particular HCD/Assembly/Sequencer using following commands within esw-shell repl

For HCDs

- `val hcdComponent = hcdCommandService("iris.hcd_name")`

For Assemblies

- `val assemblyComponent = assemblyCommandService("iris.assembly_name")`

For Sequencers

- `val sequencer = sequencerCommandService(IRIS, "darknight")`

**iris.hcd_name** and **iris.assembly_name** are the prefix by which both HCD and Assembly components were registered with location service respectively.

**IRIS** and **darknight** are the subsystem and the observing mode for the sequencer respectively.

Note - The above calls internally uses location service to resolve the required HCD/Assembly/Sequencer.

### Creating the commands to submit to HCD/Assembly

Create a setup command object using similar command to what is shown below

```scala
import csw.params.commands._
import csw.prefix.models.Prefix
import csw.params.core.models.ObsId
// above imports are available in shell, user does not need to import again

val setup = Setup(Prefix("iris.filter.wheel"),CommandName("move"),Some(ObsId("2020A-001-123")))
```

### Creating the sequence to submit to Sequencer

```scala
import csw.params.commands._
import csw.prefix.models.Prefix
import csw.params.core.models.ObsId
// above imports are available in shell, user does not need to import again

val setup = Setup(Prefix("iris.filter.wheel"),CommandName("move"),Some(ObsId("2020A-001-123")))
val sequence = Sequence(setup)
```

Other than command service handles, following pre-defined handles or factories are available in shell to interact with different services:

- For Sequence Manager, use pre-imported `sequenceManager()` handle
- For Agent, create new handle using `agentClient("iris.machine_1")`
- For AdminApi, use pre-imported `adminApi` handle
- For EventService, use pre-imported `eventService` handle
- For AlarmService, use pre-imported `alarmService` handle

### Creating ComponentId

```scala
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
// above imports are available in shell, user does not need to import again

val componentId = ComponentId(Prefix(ESW, "test1"), Assembly)
```

### Creating Event

```scala
import csw.params.core.generics.Parameter
import csw.params.core.generics.KeyType.ByteKey
import csw.prefix.models.Prefix
import csw.params.events.{EventName, SystemEvent}

val byteKey = ByteKey.make("byteKey")
val paramSet: Set[Parameter[_]] = Set(byteKey.set(100, 100 ))
val prefix = Prefix("tcs.assembly")
val event = SystemEvent(prefix, EventName("event_1"), paramSet)
```

### Creating AlarmKey

```scala
import csw.alarm.models.Key.AlarmKey
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.NFIRAOS

val alarmKey = AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneAxisHighLimitAlarm")
```

### Submitting the commands to components

Submit the setup command object created in a previous step using command service for the HCD/Assembly

- `val hcdResponse = hcdComponent.submit(setup).get`
- `val assemblyResponse = assemblyComponent.submit(setup).get`

Submit the sequence object created in a previous step using command service for the Sequencer

- `val sequencerResponse = sequencer.submit(sequence).get`

### Submitting the commands to service

To Agent

- `val agent = agentClient("iris.machine_1")`
- `val spawnResponse = agent.spawnSequenceComponent("ESW_1", Some("1.0.0")).get`

To AdminApi

- `val logMetadata = adminApi.getLogMetadata(componentId).get`

To EventService

- `val publishResponse = eventService.publish(event).get`

To AlarmService

- `val response = alarmService.setSeverity(alarmKey, AlarmSeverity.Major).get`

## Interacting with Sequence Manager

Handler to running Sequence Manager can be obtained using:

```bash
val sm = sequenceManager()
```

All Sequence Manager APIs can be called upon the handle. For example:

```bash
val configureResponse = sequenceManager.configure(ObsMode("darknight")).get
val shutdownSequencerResponse = sequenceManager.shutdownSequencer(ESW, ObsMode("darknight")).get
val resources = sm.getResources.get
```

### Provisioning sequence components

In order to provision sequence components that use some custom version of sequencer scripts, we have
provided special method in `esw-shell`:

```bash
provision(ProvisionConfig((Prefix(ESW, "primary") -> 3)), <sequencer scripts version | SHA>)
```

This is useful in case new scripts are to be tested out in dev environment. Sequencer scripts version for
these new scripts can be provided in this method. It will take care of updating this version in the config service before setting up the sequence components.
