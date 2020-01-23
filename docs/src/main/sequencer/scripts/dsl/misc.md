# Misc

## par

This utility is provided to support running multiple tasks in parallel. A call to `par` returns when all the submitted tasks complete.

The following example demonstrates sending commands in parallel to multiple HCD's.

Kotlin
: @@snip [Misc.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/MiscExample.kts) { #par }  

## Source code for examples

* [Misc Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/MiscExample.kts)