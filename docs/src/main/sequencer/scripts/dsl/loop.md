# Loops

Script supports following multiple variations of loop DSL to satisfy different use cases:

1. loop
1. waitFor
1. loopAsync

## loop - With default loop interval

`loop` DSL allows you to start loop synchronously which means rest of the code written after loop will not be executed
until `stopWhen` condition written inside loop becomes true.
You can use this DSL when you want to iteratively perform some actions until certain condition becomes true.
loop without providing any minimum interval uses default interval of `50 milliseconds`.
Default `loopInterval`is used to reduce cpu contention.

Following example demonstrate the usage of `loop` DSL without providing custom loop interval.
In the loop body, motor is being moved by 10 degrees in every iteration of the loop.
Loop will be terminated when motor's current position reaches to expected position which is `100 degrees` in this case.

Kotlin
:   @@snip [LoopExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoopExample.kts) { #loop-default-interval }  

## loop - With custom minimum loop interval

`loop` DSL allows you to provide minimum loop interval and starts loop synchronously.
Every iteration of loop will at least wait for minimum provided interval before executing next iteration.

Following example demonstrate the usage of `loop` DSL by providing custom loop interval.

Kotlin
:   @@snip [LoopExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoopExample.kts) { #loop-custom-interval }  

@@@ note

`minInterval` needs to be greater than default interval of `50 milliseconds` otherwise it will be ignored and default loop interval will be used.

@@@

## waitFor

This is a specialized version of loop and satisfies simple use cases where you want to semantically block the execution until certain condition becomes true.

In the following example, `initializeMotor` method will start the initialization and eventually set `motorUp` flag to true indicating motor is successfully initialized.
`waitFor { motorUp }` will check the value of `motorUp` flag every `50 milliseconds` and if it is true then rest of the code execution will continue.  

Kotlin
:   @@snip [LoopExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoopExample.kts) { #waitFor }

## loopAsync - With default loop interval

`loopAsync` DSL allows you to start loop asynchronously in the background which means rest of the code written after `loopAsync` will be executed concurrently.
`loopAsync` will be terminated when `stopWhen` condition written inside loop becomes true.

You can use this DSL when you want to iteratively perform some actions in the background.
loopAsync without providing any minimum interval uses default interval of `50 milliseconds`.

Following example demonstrate the usage of `loopAsync` DSL without providing custom loop interval.
In the loop body, current temperature is published every `50 milliseconds`. 
LoopAsync will be terminated when `stopPublishingTemperature` flag becomes true and this is set to true in `onStop` handler.

Kotlin
:   @@snip [LoopExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoopExample.kts) { #loopAsync-default-interval }  

## loopAsync - With custom loop interval

`loopAsync` DSL allows you to provide minimum loop interval and start loop asynchronously. 
Every iteration of loopAsync will at least wait for minimum provided interval before executing next iteration.

Following example demonstrate the usage of `loopAsync` DSL by providing custom loop interval.

Kotlin
:   @@snip [LoopExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoopExample.kts) { #loopAsync-custom-interval }

## Source code for examples

* [Loop Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoopExample.kts)