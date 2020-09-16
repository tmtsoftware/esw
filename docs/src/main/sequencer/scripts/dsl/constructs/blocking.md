# Blocking Operations Within Script

Script runs on a single thread, hence special care needs to be taken while performing blocking (CPU/IO) operations withing the script.

This section explains following two types of blocking operations and patterns/recommendations to be followed while performing those.
1. CPU Bound
1. IO Bound

Calling CPU intensive or IO operations from the main script is dangerous and should be avoided at all cost.
Breaking this rule will cause all the background tasks started in script to halt and unexpected deadlocks.

## CPU bound

For any CPU bound operations follow these steps:
1. Create a new function and mark that with `suspend` keyword
2. Wrap function body inside `withContext(Dispatchers.Default)`

Following example demonstrate writing CPU bound operation, 
In this example `BigInteger.probablePrime(4096, Random())` is CPU bound and takes more than few seconds to finish. 

Kotlin
:   @@snip [compute-intesive.kt](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/computeIntensive.kt) { #compute-intensive-function }

Following shows, usage of the above compute heavy function in main sequencer script

Kotlin
:   @@snip [ComputeIntensiveScript.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ComputeIntensiveScript.kts) { #call-compute-intensive }

## IO bound

For any IO bound operations follow these steps:
1. Create a new function and mark that with `suspend` keyword
2. Wrap function body inside `withContext(Dispatchers.IO)`

Following example demonstrate writing IO bound operation,
In this example `BufferredReader.readLine()` is IO bound and takes more than few seconds to finish.

Kotlin
:   @@snip [io-bound.kt](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/ioIntensive.kt) { #io-bound-function }

Following shows, usage of the above io heavy function in main sequencer script

Kotlin
:   @@snip [IOIntensiveScript.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/IOBoundScript.kts) { #io-bound-call }

## How does it work behind the scenes?

`withContext(Dispatchers.CPU)` or `withContext(Dispatchers.IO)` construct calls specified function on a provided dispatcher which is different from the main script.

`CPU` or `IO` dispatchers maintains separate thread pool than the main sequencer script which usage single thread.
This means, accessing/updating mutable variables defined in sequencer script is not thread safe from these functions and should be avoided.

You can read more about these patterns of blocking [here](https://medium.com/@elizarov/blocking-threads-suspending-coroutines-d33e11bf4761)
