# Sequencer Scripts

All logic in a Sequencer is implemented in Sequencer Scripts.  Scripts are written in a TMT developed
Domain Specific Language (DSL) to facilitate development based on the Kotlin programming language.
This section describes the DSL in detail.

In order to provide thread-safe concurrency, the Active Object design pattern is used for Scripts. The Active Object design pattern features a 
single “Executor” thread, in which all requests are sent to, such that only one request is processed at a time. This allows the 
Script to maintain global state variables that can be accessed in a thread-safe way.
 
@@@ note {title="Do not starve the execution thread!" }
 
The Script DSL is written to execute with a single thread. Script processing steps should not stay busy for long periods.
For instance, do not execute a CPU-bound routine on the single thread. In stead, follow patterns mentioned @ref[here](dsl/constructs/blocking.md).
 
@@@

Scripts can be written in two styles: handler-oriented and state machine-oriented.  See the page on @ref[Scripting Styles](script-styles.md)
for more information.   

@@ toc { depth=2 }


@@@ index
* [Script Styles](script-styles.md) Styles of Sequencer Scripts
* [Script DSL Constructs](dsl/script-constructs.md) DSLs for defining script and basic constructs
* [CSW Services DSLs](dsl/csw-services.md) Collection of high level DSLs for writing Sequencer scripts
@@@

@@@ note

All the examples shown in each individual section assume that you have following import in place in `script`
```kotlin
// import all the models, helpers, extensions
import esw.ocs.dsl.highlevel.models.*
```

@@@

A decision has been made to implement Scripts using a Domain Specific Language (DSL) written in Kotlin instead of true scripts in a more dynamic scripting 
language (however, we still refer to them as Scripts). This is for several reasons:

* Scripts can be developed in an IDE, with full syntax checking and auto-completion support.
* Unit tests can easily be written using widely-used testing frameworks.
* Scripts can be compiled, with compile-time error checking.
* Build tools can be used to bundle scripts into Sequencer applications.
* Code from other Scripts can be accessed and used more easily.
* Seamless integration with Common Software and Sequencer Framework.
* Kotlin features type safety (enhancing compile time error checking) and functional programming (if desired).
* Kotlin's use of *coroutines* allows for simpler procedural-style scripting without blocking.
* One less "glue" service layer implementation needed.

The most important feature of "scripting" languages that we wanted to retain is the ability to quickly modify and reload a script without 
recompiling and deploying software. Another important feature is interactivity. Scripting languages are usually based around a "shell" 
that allows interactive usage. This is provided with the `esw-shell` companion project [here](https://github.com/tmtsoftware/esw/tree/master/esw-shell)

At the ESW FDR, we planned using Scala for the scripting DSL. While this did work, the extra syntax of using Future's was cumbersome
for the scripting use case, and Kotlin provided the coroutine environment that provided the asynchronous feature of futures,
but with a much clearer syntax.

The scripting capability is in progress and we look forward to comments and suggestions from users of Release 1.
