# Sequence Component

**********

The Sequence Component is a standalone application that can load scripts and become a Sequencer.
The Sequence Component application and its arguments are described @ref[here](../../apps/sequencerapp.md).

The Sequence Component is written in Scala to provide integration with the CSW Component Framework and Services. Since component 
developers do not need to modify Sequence Component code, there is no Java version.

The Sequence Component registers itself with the Location Service when started. This allows the Sequence Manager to 
find the Sequence Component and send it commands. Sequence Component is started with a Subsystem and an optional name.
The following table describes the registration of a Sequence Component in the Location Service

| Subsystem | Name | Registered Location |
|:---------:|:----:|:-------------------:|
| esw | (none) | esw.esw_77 |
| ese | primary |  esw.primary |

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