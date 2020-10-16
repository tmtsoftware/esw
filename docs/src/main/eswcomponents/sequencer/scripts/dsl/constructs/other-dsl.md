# Other DSL

## par

This utility is provided to support running multiple tasks in parallel. A call to `par` returns when all the submitted tasks complete.

The following example demonstrates sending commands in parallel to multiple HCD's.

Kotlin
: @@snip [OtherDslExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/OtherDslExample.kts) { #par }  

## isOnline

A flag called `isOnline` is provided, which is `true` when sequencer is Online and `false` when sequencer is Offline.
This dsl is accessible in all the scopes.

Kotlin
: @@snip [OtherDslExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/OtherDslExample.kts) { #isOnline }  

## prefix

Prefix of the current sequencer is made available in all scopes by this dsl.
 
Kotlin
: @@snip [OtherDslExample.kts](../../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/OtherDslExample.kts) { #prefix }  

## Source code for examples

* [OtherDsl Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/OtherDslExample.kts)
