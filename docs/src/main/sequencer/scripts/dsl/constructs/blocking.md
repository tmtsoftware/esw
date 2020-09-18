# Blocking Operations Within Script

Script runs on a single thread, hence special care needs to be taken while performing blocking (CPU/IO) operations withing the script.

@@@ note
All the CSW related DSL's provided in the scripting environment does not block main script thread hence they can be used directly.

Exception to this here is, callback based API's for an ex. `onEvent` where you want to perform CPU/IO intensive tasks.
In this case, you need to follow one of the pattern mentioned below.
@@@

This section explains following two types of blocking operations and patterns/recommendations to be followed while performing those.

1. CPU Bound
1. IO Bound

@@@ warning { title='DO NOT BLOCK' }
Calling CPU intensive or IO operations from the main script is dangerous and should be avoided at all cost.

Breaking this rule will cause all the background tasks started in script to halt and unexpected deadlocks.
@@@

@@@ warning { title='DO NOT ACCESS/UPDATE MUTABLE STATE' }
Main sequencer script, and the techniques mentioned here for performing blocking tasks executes on different threads.

Hence, accessing/updating mutable state defined in script from these blocking functions is not thread safe.
@@@

## CPU Bound

For any CPU bound operations follow these steps:

1. Create a new function and mark that with `suspend` keyword
2. Wrap function body inside `blockingCpu` utility function

Following example demonstrate writing CPU bound operation, 
In this example `BigInteger.probablePrime(4096, Random())` is CPU bound and takes more than few seconds to finish. 

Kotlin
:   @@snip [compute-intesive.kt](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/blocking/blocking.kt) { #compute-intensive-function }

Following shows, usage of the above compute heavy function in main sequencer script

Kotlin
:   @@snip [ComputeIntensiveScript.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/blocking/ComputeIntensiveScript.kts) { #call-compute-intensive }

## IO Bound

For any IO bound operations follow these steps:

1. Create a new function and mark that with `suspend` keyword
2. Wrap function body inside `blockingIo` utility function

Following example demonstrate writing IO bound operation,
In this example `BufferredReader.readLine()` is IO bound and takes more than few seconds to finish.

Kotlin
:   @@snip [io-bound.kt](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/blocking/blocking.kt) { #io-bound-function }

Following shows, usage of the above io heavy function in main sequencer script

Kotlin
:   @@snip [IOIntensiveScript.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/blocking/IOBoundScript.kts) { #io-bound-call }

## Recommendations/Best Practices

- Create separate kotlin (.kt) file/files and maintain all the CPU/IO bound blocking tasks their
- Creating separate file/files makes sure you accidentally don't access/modify mutable state present within the script 
- Call these functions from script
- Wrap calling these function inside `async` block if you want to run them in parallel

## How does it work behind the scenes?

`blockingCpu` or `blockingIo` construct underneath uses different thread pools than the main script thread.

This means, accessing/updating mutable variables defined in sequencer script is not thread safe from these functions and should be avoided.

You can read more about these patterns of blocking [here](https://medium.com/@elizarov/blocking-threads-suspending-coroutines-d33e11bf4761)
