# Config Service

Config Service DSL is a wrapper over Config Service module provided by csw.
You can refer a detailed documentation of Config Service provided by CSW @extref[here](csw:services/config).

This DSL provides APIs to check if file exists in Configuration Service and retrieve its contents.

## existsConfig

This DSL checks if provided file exists in Configuration Service with given revision id (if provided) and returns true or false based on that.

Kotlin
:   @@snip [ConfigServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ConfigServiceDslExample.kts) { #exists-config }

## getConfig

This DSL retrieves the content of the file present at the provided path in Configuration Service. 
It returns `null` if file is not available in Configuration Service. 

In the below example, we are performing following steps:

1. Retrieve configuration file from Configuration Service
1. Fail/Terminate script if configuration file does not exist
1. Parse retrieved configuration file and convert it to `MotorCommands` domain model
1. When Sequencer receives `set-motor-speed` command, then submit `set-speed` command to downstream motor HCD
1. When Sequencer receives `rotate-motor` command, then send `set-resolution` command to downstream motor HCD

Kotlin
:   @@snip [ConfigServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ConfigServiceDslExample.kts) { #get-config }

Following example shows sample for converting `Config` object retrieved from Configuration Service to custom domain models.

Refer [this](https://github.com/lightbend/config) guide for complete usage of `Config`.

Kotlin
:   @@snip [ConfigServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ConfigServiceDslExample.kts) { #motor-commands }


### Source code for examples
* [Config Service Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ConfigServiceDslExample.kts)