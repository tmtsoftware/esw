# Blocking Operations Within Script

To avoid concurrency issues with mutable state, the scripting engine is designed as an Active Object with operations 
performing on a single thread.  Therefore special care needs to be taken while performing blocking operations 
within the script, since blocking the executing thread will also prevent other tasks, such as event handlers, from being
executed and could lead to unexpected deadlocks.

See the @ref:[Sequencer Technical Documentation](../../../technical/sequencer-tech.md) for more information on the Scripting Engine design and Active Objects.

@@@ note
All the CSW related DSL's provided in the scripting environment do not block main script thread.  Hence, they can be used directly
without worrying about stalling the script.

There is one exception to this: callback based API's, such as an `onEvent` handler.   Here, you would not want to perform 
CPU/IO intensive tasks without following one of the patterns mentioned below.
@@@

There are three types of blocking operations identified here, each with its own patterns and recommendations to be followed while performing it.

1. CPU Bound
1. IO Bound
1. Sleep

To handle the first two types of blocking operations, the DSL provide the `blockingCpu` and `blockingIo` constructs.  These
constructs cause the script execution engine to use different thread pools instead of the main script thread 
to execute the blocking code.

You can read more about these patterns of blocking [here](https://elizarov.medium.com/blocking-threads-suspending-coroutines-d33e11bf4761)

@@@ warning { title='DO NOT ACCESS/UPDATE MUTABLE STATE' }
The techniques mentioned here for performing blocking tasks causes them to be executed on a different thread than the main 
sequencer script.  Therefore, accessing/updating mutable state defined in the main script from these blocking functions is not thread safe.

We recommend creating separate kotlin (.kt) file/files and maintaining all the CPU/IO bound blocking tasks there, to reduce
the risk of improperly accessing mutable state.
@@@

## CPU Bound

For any CPU bound operations follow these steps:

1. Create a new function and mark it with the `suspend` keyword.
1. Wrap the function body inside a `blockingCpu` utility function.
1. Call the new function.  Wrap it in an `async` block to run it in the background.

The following example demonstrates writing a CPU bound operation. 
In this example, the method `BigInteger.probablePrime(4096, Random())` is CPU bound and takes more than few seconds to finish.
We wrap it in another method, `findBigPrime` and mark it with the `suspend` keyword.

Kotlin
:   @@snip [compute-intensive.kt](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/blocking/blocking.kt) { #compute-intensive-function }

The following shows the usage of the above compute heavy function in main sequencer script in two ways.  First it is called
in the main script flow causing the main script to pause until the method completes with a value.  Note that background
tasks continue to execute while the method is being executed.  In the second way, an `async` block is used, which causes
the method to be executed in the background.  The `async` block returns a `Deferred` type, and the `Deferred.await()` 
method can be used to pause script execution until the `async` block completes without blocking the other background processes.

Kotlin
:   @@snip [ComputeIntensiveScript.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/blocking/ComputeIntensiveScript.kts) { #call-compute-intensive }

## IO Bound

For any IO bound operations follow these steps:

1. Create a new function and mark that with `suspend` keyword.
1. Wrap function body inside `blockingIo` utility function.
1. Call the new function.  Wrap it in an `async` block to run it in the background.

The following example demonstrates writing IO bound operation.
In this example the method `BufferredReader.readLine()` is IO bound and takes more than few seconds to finish.
We wrap it in another method, `readMessage` and mark it with the `suspend` keyword.


Kotlin
:   @@snip [io-bound.kt](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/blocking/blocking.kt) { #io-bound-function }

The following shows the usage of the above IO heavy function in main sequencer script, again in two ways: in main script
flow and also using an `async` block to execute in the background.

Kotlin
:   @@snip [IOIntensiveScript.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/blocking/IOBoundScript.kts) { #io-bound-call }

## Sleep

There may be cases where you simply need to pause execution of your script for a specific amount of time.  Using a traditional
`sleep` or `wait` command performs as expects, pausing the executing thread, but due to the nature of the Active Object,
these block background handlers as well.  Therefore, the Kotlin Coroutine package provides a method, `delay`, that should be used 
to prevent this blocking.  Like `sleep`, `delay` takes the number of milliseconds to pause as an argument. 