# Sequencer State Transition

Sequencer is implemented as a Finite State Machine. It has a set of states, and at any given time it could be in exactly
one of those states. It supports a set of commands/messages, and on receiving those commands, it
might choose to go in some other state. 

Following are the states supported by the Sequencer:

* **Idle/Online:** This is the default state of the Sequencer. A Sequencer is idle when it is up, but there's no sequence under execution.
A sequencer can come to idle state from the following situations:

    * when the sequencer comes up for the first time
    * when the sequencer has finished execution of a sequence
    * when the sequencer was offline, and a goOnline command is sent

* **Loaded:** A sequencer is in loaded state when a sequence is loaded for execution, but execution of the sequence hasn't started.
A separate `start` command is expected to start execution of the sequence. 
All sequence editor actions (for e.g. add, remove, reset) are accepted in this state.
From this state, the sequencer can go in `InProgress` state
on receiving a `start` command, or it could go in `offline` state if `goOffline` command is sent. On receiving a `reset` command,
which discards all the pending steps, the sequencer will go to `idle` state.
 
* **InProgress/Running:** The Sequencer is in `running` state, only when it is executing a sequence. All sequence editor actions
(for e.g. add, remove, reset) are accepted in this state. From `running` state, the sequencer can go to `idle` state on completion of sequence,
or it can be `killed`. In order to go `offline` from this state, the sequencer has to go to `idle` and then `offline`.

* **Offline:** The sequencer goes in `offline` state, only on receiving a `goOffline` command which can either come from an upstream
sequencer, or from a user through the admin dashboard. In this state, only a few commands are excepted (for eg. goOnline, shutdown, status etc).   

* **Killed:** This is the final state of the sequencer. The shutdown command can be sent in any state, hence a sequencer can transition to this state
from any other state. 


![sequencer-state-transition](state-transition.png)