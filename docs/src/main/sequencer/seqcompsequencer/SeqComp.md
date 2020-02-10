# Sequence Component

The Sequence Component is a standalone application that can load scripts and become a Sequencer.
The Sequence Component application and its arguments are described @ref[here](../../apps/sequencerapp.md).

The Sequence Component is written in Scala to provide integration with the CSW Component Framework and Services. Since component 
developers do not need to modify Sequence Component code, there is no Java version.

The Sequence Component registers itself with the Location Service when started. This allows the Sequence Manager to 
find the Sequence Component and send it commands. Sequence Component is started with a Subsystem and an optional name.
The following table describes the registration of a Sequence Component in the Location Service

| subsystem | name | registered location |
|:---------:|:----:|:-------------------:|
| esw | (none) | esw.esw_77 |
| ese | primary |  esw.primary |

The Sequence Component provides framework code to support the loading and unloading of Scripts and a few other 
useful capabilities.

| message | arguments | description |
|:-------:|:----------|:----------|
| LoadScript| subsystem: Subsystem<br>observingMode: String | Load a script in the Sequence<br>Component to create a Sequencer |

```
 sealed trait SequenceComponentMsg extends OcsAkkaSerializable
 final case class LoadScript(subsystem: Subsystem, observingMode: String, replyTo: ActorRef[ScriptResponse]) extends SequenceComponentMsg
 final case class UnloadScript(replyTo: ActorRef[Done])      extends SequenceComponentMsg
 final case class Restart(replyTo: ActorRef[ScriptResponse]) extends SequenceComponentMsg
 final case class GetStatus(replyTo: ActorRef[GetStatusResponse]) extends SequenceComponentMsg
 
```
 

When the SequencerComponent is constructed, it is registered in the Location Service using the Sequencer name passed into the application. 
This allows external entities to locate this actor and send it the LoadScript message. When a script is loaded, the Wiring registers the 
Supervisor actor with the Location Service separately using the name specific to the script loaded. This allows Sequencer 
commands (e.g. submit, start, pause, resume, etc.) to be sent directly to the Supervisor. Note that the wiring is specific to the script. 
When a StopScript message is sent to the SequencerComponent actor, the wiring is torn down, and the SequencerComponent returns to the idle state.
The LoadScript message indicates the new observing mode for the Sequencer. The application will load the observing mode 
to Script map, which is also stored in the Script Service, to identify the appropriate Script class, which is loaded using the Java class loader by reflection.