# Command Service

Command service dsl is kotlin wrapper over csw command service module provided for sending commands to assemblies or hcds via scripts.
This dsl exposes following APIs:

## Assembly

This dsl resolves assembly with provided name and gives handle to command service dsl through which script can interact with
assembly. For example send commands or lifecycle methods e.g. goOnline, goOffline, lock assembly etc. This api also takes default timeout
which will be used in commands like submitAndWait, queryFinal etc.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #assembly }

## Hcd

This dsl resolves hcd with provided name and gives handle to command service dsl through which script can interact with hcd. For example
send commands to assembly or lifecycle methods e.g. goOnline, goOffline, lock hcd etc.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #hcd }

## Submit

This dsl allows to submit a command to assembly/hcd and return after first phase. If it returns Started then final response can
be obtained with query final api.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-component }

submit always returns positive submit response. In case of negative submit response, onError handler (if written) is called and then
onGlobalError handler is called. Script execution flow breaks in case of negative submit response and sequence is terminated with failure. 
Following example shows scenario where script execution flow breaks when submit return negative response, in this case onError handler will be executed followed by
onGlobalError handler, and sequence is completed with failure.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-component-on-error }

To change this default behaviour, resumeOnError flag can be used. If this flag is set to true then script execution continues, and action is taken based on custom logic
in script. Script writer can still choose to terminate sequence using `failedOnTerminate` utility.

Kotlin
:   @@snip [CommandServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/CommandServiceDslExample.kts) { #submit-component-error-resume }

 

