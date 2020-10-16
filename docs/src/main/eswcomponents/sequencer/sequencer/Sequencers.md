# Sequencers

Sequencers all use the same component framework. What makes a Sequencer unique is the Script it is loaded with. 
A Sequencer is a Sequence Component configured with a specific Script. The Script is written with a specific observing mode 
(or set of common observing modes) in mind. Which observing modes a Script can support is up to the developer, but the intention is that a Script can be 
developed independently of other Scripts to refine behavior specific to an observing mode, without affecting any other observing modes.
Behavior that is common to more than one observing mode can be defined in Scripts that can be shared and "imported" into other scripts 
(see @ref:[ReusableScripts](../scripts/dsl/constructs/define-script.md#reusable-scripts)).

Since the Script defines the behavior of the Sequencer, one can be written to support a simulation mode or a standalone mode, 
such that development and testing can be performed with a Sequencer handling real Sequences, but only simulating its actions.
Scripts can also be created for special purposes such as testing Assemblies or HCDs in the lab.

## Defining Observing Modes

Usually, observing modes are associated with instruments or engineering tasks. Observing modes must following a naming convention
shown below:

```
              <system>_<modeName>

                   IRIS_ifsonly
                   WFOS_darknight
                    ENG_pointingmap
```

The `<system>` indicates the system to which the observing mode applies to.  This is typically the instrument subsystem
used for the observations, but could be any subsystem or some other tag (such as ENG) that identifies the observing mode.
If a subsystem is used, it should be capitalized to conform with other subsystem uses. 
The `<modeName>` portion will generally be indicated by a selection in the observation planning tool. There are no restrictions on
this name, but shorter is better. 

## Registering Sequencers in Location Service

A Sequencer is a Sequence Component that has a script loaded. A Sequencer converts a Sequence Component, but the Sequence Component stays around.
A Sequencer must be started specifying the subsystem for the Sequencer and the observing mode (as described above). 

Like Sequence Components, Sequencers register themselves in the Location Service based on arguments used when they are 
started. The following table shows scenarios that may happen when the Sequence Manager starts Sequencers for an observing mode.

<!-- Comment: span tag in Registered Location is to fix broken link error.
Use of @ sign makes it appear like an email, hence validateSite fails without span tag.
-->

| Sequence<br>Component Name | Sequencer Subsystem | Observing Mode| Registered Location | Description|
|:--------------------------:|:---------:|:-------------:|:-------------------:|:-----------|
| ESW.ESW_77 | IRIS | IRIS_ifsonly |IRIS<span>@</span>IRIS_ifsonly | An IRIS instrument Sequencer running the IRIS script for the IRIS_ifsonly observing mode using the ESW.ESW_77 Sequence Component. |
| ESW.primary | ESW |  IRIS_ifsonly | ESW<span>@</span>IRIS_ifsonly | An ESW Sequencer running the ESW script for the IRIS_ifsonly observing mode using the ESW.primary Sequence Component. |

As shown above, the observing mode is the instrument name and an instrument-specific label related to observing mode features. Each instrument 
includes its scripts inside its specific package. The subsystem and observing mode are used to lookup the correct script in the script repository. 
Once the Sequencer script is loaded in a Sequence Component, the Sequencer API exposes a `GetSequenceComponent` command which returns the
Location of the Sequence Component allowing the Sequence Manager or other client to determine which Sequence Component is executing
the observing mode script for a specific packageId.

The figure below shows the Sequence Component ESW_77 loading the IRIS instrument script for the IRIS_ifsonly observing mode.

![SequenceCompNaming](../../images/ocs/OCS-SeqCompSeqNaming.png)

Once loaded, a client can ask the Sequence Component what Sequencer it is running using the `GetStatus` message. A client can ask
what Sequence Component a Sequencer is running on using the `GetSequenceComponent` message. Both commands return a Location Service `Location`.
The naming convention allows the following:

1. The Sequence Manager can search for all Sequencers related to an observing mode.
2. The Sequence Manager can identify the Sequencer running a subsystem’s instrument observing mode script.
3. The Sequence Manager can identify which Sequence Component is running a specific subsystem’s instrument observing mode. 


## Sequencer Technical Design

See @ref:[Sequencer Technical Documentation](../../../technical/sequencer/sequencer.md).
