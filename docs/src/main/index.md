# TMT Executive Software (ESW)

@@@ index

- [Writing Sequencer Scripts](sequencer/scripts/scripts.md)
- [Sequencer State Transition](sequencer/state-transition.md)
- [Applications](apps/apps.md)
@@@

Three main components are delivered as part of ESW Phase 1:

* **Sequencer:** This allows users to create a `Sequencer` component. Both top-level Sequencer (OCS)
and subsystem Sequencers can be created. Allows domain experts to write custom scripts
for each sequencer in a Kotlin based domain specific language. All sequencer scripts will be written
in the [Sequencer Scripts Repo](https://github.com/tmtsoftware/sequencer-scripts).
* **Sequence Component:** The Sequence Component is used to spawn and shutdown sequencers dynamically.

* **ESW Gateway:** This is provided to give access to all CSW and ESW services and components from the
browser.

@@@ note
Executive Software (ESW) is a reimplementation/refactoring of the prototype ESW code [here](https://github.com/tmtsoftware/esw-prototype) 
developed during the ESW design phase with changes to make the code and public APIs
more robust and resilient and to improve its usability and performance for use at the
TMT Observatory.
@@@