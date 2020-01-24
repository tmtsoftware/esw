# Sequencer Scripts

All logic in Sequencer is implemented in Sequencer scripts.  Scripts are written in in Kotlin using a TMT developed
Domain Specific Language (DSL) to facilitate development.  This section describes the DSL in detail. 

As an alternate to procedural style scripting, some framework DSL has been provided to allow scripting using a 
Finite State Machine (FSM) (still in development).  

@@ toc { .main depth=1 }

@@@ index

* [Scripting DSL](dsl/dsl.md) Collection of high level DSL's for writing Sequencer scripts
* [FSM](fsm.md) How to create a finite state machine using a Sequencer script
@@@

@@@ note

All the examples shown in each individual section assume that you have following import in place in `script`
```kotlin
// import all the models, helpers, extensions
import esw.ocs.dsl.highlevel.models.*
```

@@@