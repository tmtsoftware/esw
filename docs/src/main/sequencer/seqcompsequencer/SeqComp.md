# Sequence Component

The Sequence Component is a standalone application that can load scripts and become a Sequencer.
The Sequence Component application and its arguments are described @ref[here](../../apps/sequencerapp.md).

The Sequence Component is written in Scala to provide integration with the CSW Component Framework and Services. Since component 
developers do not need to modify Sequence Component code, there is no Java version.

The Sequence Component registers itself with the Location Service when started. This allows the Sequence Manager to 
find the Sequence Component and send it commands. Sequence Component is started with a Subsystem and an optional name.
While the Subsystem for a Sequence Component identifies to which subsystem the Sequence Component belongs, a Sequence
Component can load Scripts for any subsystem, and therefore become a Sequencer for any subsystem.  For example, if for
some reason the IRIS Sequence Component is not reachable, an IRIS Script can be loaded into an ESW Sequence Component 
and it can then be used as the IRIS Instrument Sequencer.

The following table describes the registration of a Sequence Component in the Location Service

| Subsystem | Name | Registered Location |
|:---------:|:----:|:-------------------:|
| ESW | (none) | ESW.ESW_77 |
| ESW | primary |  ESW.primary |

Note that CSW always capitalizes a subsystem when it is displayed. Arguments can be entered as lowercase.

The Sequence Component provides framework code to support the loading and unloading of Scripts and a few other 
useful capabilities.

| Message | Description |
|:-------:|:----------|
| LoadScript| Load a script in the Sequence Component to create a Sequencer. Takes a subsystem and observing mode as arguments. |
| UnloadScript|Unloads a loaded script returning a Sequence Component. |
| Restart | Unloads and reloads a script causing initialization of state. |
| GetStatus | Returns the Location of the Sequence Component's loaded Sequencer |

For more details on the messages handled by the Sequence Component see [here]($github.base_url$/esw-ocs/esw-ocs-impl/src/main/scala/esw/ocs/impl/messages/SequenceComponentMsg.scala)


## Sequence Component Technical Design

This section will be updated with the Sequence Component technical design information in a future release.