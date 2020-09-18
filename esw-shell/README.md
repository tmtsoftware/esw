# esw-shell

This project contains an interactive shell and allows its users to gain access to all the major csw services via CLI 
which then can be used to communicate with a HCD (Hardware Control Daemon) and an Assembly using 
TMT Common Software ([CSW](https://github.com/tmtsoftware/csw)) APIs and with a Sequencer using 
TMT Executive Software ([ESW](https://github.com/tmtsoftware/esw)). 

## Build Instructions

The build is based on sbt and depends on libraries generated from the 
[csw](https://github.com/tmtsoftware/csw) and [esw](https://github.com/tmtsoftware/esw) project.

## Prerequisites for running Components

The CSW services need to be running before starting the components. 
This is done by starting the `csw-services.sh` script, which is installed as part of the csw build.
If you are not building csw from the sources, you can get the script as follows:

 - Download csw-apps zip from https://github.com/tmtsoftware/csw/releases.
 - Unzip the downloaded zip.
 - Go to the bin directory where you will find `csw-services.sh` script.
 - Run `./csw_services.sh --help` to get more information.
 - Run `./csw_services.sh start` to start the location service and config server.

## Running the esw-shell using sbt

After making sure that all the pre-requisites are satisfied, we can directly run the esw-shell via sbt 
from the root directory of the project

 - Run `sbt esw-shell/run` 
 
## Running esw-shell using Coursier

Execute the following command to launch esw-shell.(`<version>` can be also be SHA)

```
cs launch --channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json esw-shell:<version>
```

You can add the TMT apps channel to your Coursier by below command.
```
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json
```

After adding TMT apps channel you can simply launch esw-shell by executing
```
cs launch esw-shell:<version | SHA>
```

## Usage of Command Service to interact with HCDs, Assemblies and Sequencers 

### Finding the required component

Get handle to the command service for a particular HCD/Assembly/Sequencer using following commands within esw-shell repl
 - For HCDs
 `val hcdComponent = hcdCommandService("iris.hcd_name")`
 - For Assemblies
 `val assemblyComponent = assemblyCommandService("iris.assembly_name")`
 - For Sequencers
 `val sequencer = sequencerCommandService(IRIS, "darknight")`

Get handle to the services like SequenceManager/Agent using following commands within esw-shell repl
 - For Sequence Manager
 `val sequenceManager = sequenceManagerService()`
 - For Agent
 `val agent = agentClient("iris.machine_1")`
 - For AdminApi
 `adminApi`
 - For EventService
 `eventService`
 - For AlarmService
 `alarmService`
 
**iris.hcd_name** and **iris.assembly_name** are the prefix by which both HCD and Assembly components were registered 
with location service respectively.

**IRIS** and **darknight** are the subsystem and the observing mode for the sequencer respectively.

Note - The above calls internally uses location service to resolve the required HCD/Assembly/Sequencer.

### Creating the commands to submit to HCD/Assembly

Create a setup command object using similar command to what is shown below

```scala
import csw.params.commands._
import csw.prefix.models.Prefix
import csw.params.core.models.ObsId

val setup = Setup(Prefix("iris.filter.wheel"),CommandName("move"),Some(ObsId("sample-obsId")))
```

### Creating the sequence to submit to Sequencer

```scala
import csw.params.commands._
import csw.prefix.models.Prefix
import csw.params.core.models.ObsId

val setup = Setup(Prefix("iris.filter.wheel"),CommandName("move"),Some(ObsId("sample-obsId")))
val sequence = Sequence(setup)
```

### Creating the componentId to getLogMetadata by using AdminApi

```scala
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem
import csw.location.api.models.{ComponentId, ComponentType}

val componentId = ComponentId(Prefix(Subsystem.ESW, "test1"), ComponentType.Assembly)
```

### Creating the event to publish by using eventService

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

### Creating the alarmKey to setSeverity by using alarmService

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

To Sequence Manager
 - `val sequenceManager = sequenceManagerService()`
 - `val configureResponse = sequenceManager.configure(ObsMode("darknight")).get`
 - `val shutdownSequencerResponse = sequenceManager.shutdownSequencer(ESW, ObsMode("darknight")).get`
 
To Agent
 - `val agent = agentClient("iris.machine_1")`
 - `val spawnResponse = agent.spawnSequenceComponent("ESW_1", Some("1.0.0")).get`

To AdminApi
 - `val logMetadata = adminApi.getLogMetadata(componentId).get`
 
To EventService
 - `val publishResponse = eventService.publish(event).get`
 
To AlarmService
 - `val response = alarmService.setSeverity(alarmKey, AlarmSeverity.Major).get`
