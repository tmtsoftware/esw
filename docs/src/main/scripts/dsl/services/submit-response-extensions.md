# Extension utilities on SubmitResponse 

A `submit` or `query` (and other Command Service or Sequencer Command Service API calls) always return a positive `SubmitResponse`
unless called with `resumeOnError` flag as `true`.  
For `submit`, the two possible positive responses are `Started` and `Completed`.
They can be handled using the `.onStarted` and `.onCompleted` methods, respectively.
These methods allow you to specify a block of code to be called in each of those cases. 

Kotlin
: @@snip [SubmitResponseExtensions.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SubmitResponseExtensions.kts) { #extensions }

Alternatively, a Kotlin `when` can be used to perform pattern matching on the result. An example of that is 
shown below.

Kotlin
: @@snip [SubmitResponseExtensions.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SubmitResponseExtensions.kts) { #kotlin-when }

If you desire to handle errors manually on a per-command basis, the `resumeOnError` flag can be used. If this flag is set to true,
then script execution continues, and action is taken based on custom logic in script by using an `.onFailed` method.

Kotlin
: @@snip [SubmitResponseExtensions.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SubmitResponseExtensions.kts) { #onFailed }


You can still choose to terminate sequence using the `onFailedTerminate` utility.
This will cause similar behavior as when flag is not set by calling the `onError` or `onGlobalError` blocks and terminating the sequence,
if the `SubmitResponse` is some kind of error.

Kotlin
: @@snip [SubmitResponseExtensions.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/SubmitResponseExtensions.kts) { #onFailedTerminate }
