# Logging Service

The Logging Service DSL is a wrapper for the Logging Service module provided by CSW. 
You can refer to the detailed documentation of Logging Service provided by CSW @extref[here](csw:services/logging).

The CSW documentation explains all the supported logging related configurations for example, default log level, component specific log levels, log appender etc.
It also explains how to override default values.

All Sequencer scripts are expected to be kept inside the [sequencer-scripts](https://github.com/tmtsoftware/sequencer-scripts) repo under Subsystem specific directories.
Read more about adding new scripts and script specific configuration files [here](https://github.com/tmtsoftware/sequencer-scripts).

## Changing Sequencer Log Level

Let's say you are writing an IRIS sript for the "darknight" observing mode (`IrisDarknight.kts`). You place this script in `scripts/iris` directory in [sequencer-scripts](https://github.com/tmtsoftware/sequencer-scripts) repo.
In the same directory, you can create your Subsystem specific configuration file, in this case `iris.conf` file.

Add following snippets in `iris.conf` file to change the default log level for the IRIS darknight script to **debug** :

```hocon
# iris.conf

csw-logging {
  component-log-levels {
    IRIS.darknight = debug
  }
}

```

Once you add this configuration in `iris.conf` file, next step is to include this configuration file in `application.conf` file present at 
`scripts/application.conf` location in the [sequencer-scripts](https://github.com/tmtsoftware/sequencer-scripts) repo as shown in below snippet:

```hocon
# application.conf

include "iris.conf"
```

@@@ note

**IRIS** - name of the Sequencer/Script Subsystem provided while starting Sequencer App

**darknight** - observing mode provided while starting Sequencer App or loaded into Sequence Component

@@@

Logging Service DSL exposes following APIs to script writers for logging at different levels:

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