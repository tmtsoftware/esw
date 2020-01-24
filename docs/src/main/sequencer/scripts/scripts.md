# Sequencer Scripts

All logic in Sequencer is implemented in Sequencer scripts.  Scripts are written in in Kotlin using a TMT developed
Domain Specific Language (DSL) to facilitate development.  This section describes the DSL in detail. 

As an alternate to procedural style scripting, some framework DSL has been provided to allow scripting using a 
Finite State Machine (FSM) (still in development).  


@@@ index
* [Script DSL Constructs](dsl/script-constructs.md) DSLs for defining script and basic constructs
* [CSW Services DSLs](dsl/csw-services.md) Collection of high level DSLs for writing Sequencer scripts
@@@

* Script DSL Constructs
    * @ref[Script Handlers](dsl/constructs/handlers.md)
    * @ref[Looping](dsl/constructs/loop.md)
    * @ref[Other DSL](dsl/constructs/misc.md)
    * @ref[Finite State Machines](dsl/constructs/fsm.md)
* CSW Services DSL
    * @ref[Location Service](dsl/services/location-service.md)
    * @ref[Config Service](dsl/services/config-service.md)
    * @ref[Assembly/HCD Command Service](dsl/services/command-service.md)
    * @ref[Sequencer Command Service](dsl/services/sequencer-command-service.md)
    * @ref[Submit Response Extensions](dsl/services/submit-response-extensions.md)
    * @ref[Event Service](dsl/services/event-service.md)
    * @ref[Logging Service](dsl/services/logging-service.md)
    * @ref[Alarm Service](dsl/services/alarm-service.md)
    * @ref[Time Service](dsl/services/time-service.md)


@@@ note

All the examples shown in each individual section assume that you have following import in place in `script`
```kotlin
// import all the models, helpers, extensions
import esw.ocs.dsl.highlevel.models.*
```

@@@