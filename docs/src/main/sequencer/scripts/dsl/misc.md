# Misc

## par

This utility is provided to support running multiple tasks in parallel. Call to `par` returns when all the submitted task completes.

Following example demonstrate a use case of sending commands in parallel to multiple HCD's. 

Kotlin
:   @@snip [Misc.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/MiscExample.kts) { #par }  

### Source code for examples
* @github[Misc Examples](/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/MiscExample.kts)