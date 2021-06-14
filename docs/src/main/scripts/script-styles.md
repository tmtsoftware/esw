# Sequencer Script Styles

Scripts can be written in two styles: handler-oriented or state machine-oriented.  The choice is determined by
how the developer wants their Sequencer to work.

### Handler-Oriented Scripts

A handler-oriented script is specified with command-handlers at the top-level scope of the script. There is a
command-handler associated with every possible Setup or Observe that a received Sequence can contain. The
developer can use mutable variables in the script to keep track of the state of the Sequencer script.

A handler-oriented script must be started using the `script` keyword indicating a scope. The following brief
example shows the structure of a handler-oriented script.

```
script {
    val wfosSequencer = Sequencer(WFOS, "wfos.bluearm", Duration.seconds(10))
    val wfosBlueDetAssembly = Assembly(WFOS, "blueDetectorAssembly", Duration.seconds(10))
    var wfosState = IDLE

    onSetup("wfos-command-1") {
      // Start actions for wfos-command-1
      wfosState = BUSY 
    }

    onSetup("wfos-command-2") {
      // Start actions for wfos-command-2
      if (wfosState == BUSY) {
        // Reject
      } else {
        // Do some IDLE action
    }
}
``` 

This example shows two Setup handlers. One for `wfos-command-1` and one for `wfos-command-2`. These names are compared with the `CommandName`
fields of incoming Setups in the Sequence. Note that the `var` represents internal state. The command-handlers must maintain
their state and perform checks as needed to properly handle commands.  Code can also be added outside of handlers.  This 
code will be run when the script is loaded.  This can be used to, for example, set up subscriptions to Events which can be
manually associated with internal state variables or other actions.

More information on handlers is available @ref:[here](dsl/constructs/handlers.md).

### State Machine-Oriented Scripts

A State Machine-oriented Script models the entire script as a state machine. The script developer writes a number 
of `state` entries that make sense for the Sequencer. Logic can be tied to events or commands. Within a `state` 
one can define command handlers to process Sequence steps. The command handlers within a `state` only work when
the state machine is in that state. 

A state machine-oriented script must start with the FsmScript keyword. The argument of FsmScript is the initial
state machine state. Like handler-oriented scripts, state machine-oriented scripts can contain state that can be 
shared across states. The following example shows the most important features of an FsmScript. 

```
FsmScript("OFF") {

    state("ON") { params ->

        onSetup("turn-off") {
            turnOffLight()
            become("OFF") 
        }
    }

    state("OFF") {

        onSetup("turn-on") { command ->
            turnOnLight()
            become("ON", command.params)
        }
    }

}

```
In this script, the states called *ON* and *OFF* are defined. The initial state is *OFF*. In the *ON* state, the script can
process the *turn-off* command. It executes the `turnOffLife` function and switches to the ON state using the `become` keyword.

More information about state machine-oriented scripts is @ref:[here](dsl/constructs/fsm.md).  


### Mixing the Two Styles

A handler-oriented script can include finite state machines.  The top-level script is not modeled as a state machine, but
the command handlers and script can include one or more state machines and can launch them when a command is received.

A state machine-oriented script can receive commands with handlers as shown in the example above. A state machine-oriented
script can start/stop/control other finite state machines as it processes the top-level state machine.

<!-- we should include an example of this.  how are the script and FsmScript tags used? -->
