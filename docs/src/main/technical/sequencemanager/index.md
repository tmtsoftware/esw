# Sequence Manager

Sequence Manager is a service that is part of the Observatory Control System (OCS) subsystem of ESW.
Sequence Manager has the following high-level responsibilities in the ESW.OCS design:

* Manage and track Observatory resources. Ensure that the resources needed for a Sequence are available allowing the Sequence to execute.
* Start Sequence Components required by a specific Sequence and initialize each with correct Script.
* Monitor overall Sequence execution and perform cleanup at the conclusion of a Sequence.

The following sections provide details on the Sequence Manager.

@@ toc { depth=2 }

@@@ index

* [Sequence Manager Introduction](sequence-manager.md)
* [Sequence Manager Implementation Details](sm-impl-details.md)
* [Sequencer Manager States](sm-state-transition.md)

@@@
