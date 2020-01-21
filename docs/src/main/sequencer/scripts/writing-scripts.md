# Sequencer Script

@@ toc { .main depth=1 }

Framework support three types of script:

1. **FSM Script** : Top level script itself is **Finite State Machine Script** where top-level script will have multiple
states. Each state will handle specific commands and can transition to next state depending on logic written in script. For
details about writing top-level FSM script, please refer @ref:[here](fsm-script.md)

2. **Script** : Top level script can handle all commands for which handlers are defined. This script can contain another FSM
but this contained FSM won't handle commands. For details about writing Script, please refer @ref:[here](script.md)

3. **Reusable Scripts**: Script writer can extract reusable parts in this script. This reusable script can be loaded in other **Scripts**.
For details about writing Reusable Script, please refer @ref:[here](reusable-script.md)

Framework also provides DSL support for scripts which is explained in detailed @ref:[here](dsl/dsl.md)
