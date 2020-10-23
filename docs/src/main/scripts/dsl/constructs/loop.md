# Including Looping in Scripts

The script DSL supports a variety of looping constructs to satisfy different use cases:

1. loop
1. waitFor
1. loopAsync

### loop - With Default loop Interval

The `loop` DSL allows you to start a "blocking" loop so that the rest of the code after loop will not be executed
until a `stopWhen` condition written inside loop becomes true.
You can use this DSL when you want to iteratively perform some actions until a certain condition becomes true.
An interval can be provided to set the minimum period of the loop, where every iteration of the loop wait at least for the minimum 
provided interval before executing the next iteration.  If no period is provided, the default interval is `50 milliseconds`.

The following example demonstrates the usage of the `loop` DSL with the default interval.
In the loop body, a motor is being "moved" by 10 degrees in every iteration of the loop.
The loop will be terminated when motor's current position reaches the expected position of 100 degrees.

Kotlin
:   @@snip [LoopExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoopExample.kts) { #loop-default-interval }  

### loop - With Custom Minimum Loop Interval

The following example demonstrates the usage of the `loop` DSL when providing a custom loop interval.

Kotlin
:   @@snip [LoopExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoopExample.kts) { #loop-custom-interval }  

@@@ note { title="Is there a minimum loop interval?" }

`minInterval` needs to be greater than default interval of `50 milliseconds` otherwise it will be ignored and default loop interval will be used.

@@@

### waitFor - Loop Until a Condition is True

This is a specialized version of loop and satisfies simple use cases where you want to semantically block the 
execution until certain condition becomes true.

In the following example, `initializeMotor` method will start the initialization and eventually set `motorUp` flag to true indicating motor is successfully initialized.
`waitFor { motorUp }` will check the value of `motorUp` flag every `50 milliseconds`, and when it is true, the rest of the code execution will continue.  

Kotlin
:   @@snip [LoopExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoopExample.kts) { #waitFor }

### loopAsync - With a Default Loop Interval

The previous DSL loop constructs "block" until the loop completes. You can use this DSL when you want to iteratively perform some actions in the background.
Since it is asynchronous, once the code in the background loop starts, the code written after `loopAsync` will be executed immediately and concurrently.

Like `loop`, `loopAsync` will be terminated when a `stopWhen` condition written inside loop becomes true. `loopAsync` also 
has a default interval of `50 milliseconds`.

The following example demonstrates the usage of `loopAsync` DSL with the default interval.
In the loop body, a current temperature event is published every `50 milliseconds`. 
The `loopAsync` will be terminated when `stopPublishingTemperature` flag becomes true, which is set to true in the `onStop` handler.

Kotlin
:   @@snip [LoopExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoopExample.kts) { #loopAsync-default-interval }  

### loopAsync - With a Custom Loop Interval

The following example demonstrates the usage of `loopAsync` DSL with a custom loop interval specified.

Kotlin
:   @@snip [LoopExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoopExample.kts) { #loopAsync-custom-interval }

## Source code for examples

* [Loop Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoopExample.kts)
