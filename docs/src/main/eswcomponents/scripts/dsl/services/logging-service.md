# Logging Service

The Logging Service DSL is a wrapper for the Logging Service module provided by CSW.
You can refer to the detailed documentation of Logging Service provided by CSW @extref[here](csw:services/logging).

The CSW documentation explains all the supported logging related configurations for example, default log level, component specific log levels, log appender etc.
It also explains how to override default values.

All Sequencer scripts are expected to be kept inside the [sequencer-scripts](https://github.com/tmtsoftware/sequencer-scripts) repo under Subsystem specific directories.
Read more about adding new scripts and script specific configuration files [here](https://github.com/tmtsoftware/sequencer-scripts).

The default log level for sequencers can be set using command line options.  See the @ref[SequencerApp documentation](../../../../apps/sequencer-app.md#setting-the-default-log-level) for more information.

The Logging Service DSL exposes following APIs to script writers for logging at different levels:

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

## fatal

Kotlin
:   @@snip [LoggingServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoggingServiceDslExample.kts) { #fatal }

## Source code for examples

* [Logging Service Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LoggingServiceDslExample.kts)
