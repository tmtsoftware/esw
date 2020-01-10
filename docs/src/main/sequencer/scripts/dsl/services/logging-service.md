# Logging Service

Logging Service DSL is a wrapper over logging service module provided by csw. 
You can refer a detailed documentation of Logging Service provided by csw @extref[here](csw:services/logging).

This dsl exposes following APIs to script writers for logging at different levels:

## trace

Kotlin
:   @@snip [LoggingServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoggingServiceDslExample.kts) { #trace }

## debug

Kotlin
:   @@snip [LoggingServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoggingServiceDslExample.kts) { #debug }

## info

Kotlin
:   @@snip [LoggingServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoggingServiceDslExample.kts) { #info }

## warn

Kotlin
:   @@snip [LoggingServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoggingServiceDslExample.kts) { #warn }

## error

Kotlin
:   @@snip [LoggingServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoggingServiceDslExample.kts) { #error }

