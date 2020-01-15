# Config Service

Config Service Dsl is a wrapper over config service module provided by csw.
You can refer a detailed documentation of Config Service provided by csw @extref[here](csw:services/config).

This dsl provides APIs to check if file exists in configuration service and retrieve its contents.

## existsConfig

This DSL checks if provided file exists in configuration service with given revision id (if provided) and returns true or false based on that.

Kotlin
:   @@snip [ConfigServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ConfigServiceDslExample.kts) { #exists-config }

## getConfig

This DSL retrieves the content of the file present at the provided path in configuration service. 
It returns `null` if file is not available in configuration service. 

In the below example, we are performing following steps:

1. Retrieve configuration file from configuration service
1. Fail/Terminate script if configuration file does not exist
1. Parse retrieved configuration file and convert it to `MotorCommands` domain model
1. When sequencer receives `set-motor-speed` command, then submit `set-speed` command to downstream motor hcd
1. When sequencer receives `rotate-motor` command, then send `set-resolution` command to downstream motor hcd

Kotlin
:   @@snip [ConfigServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ConfigServiceDslExample.kts) { #get-config }

Following example shows sample for converting `Config` object retrieved from configuration service to custom domain models.

Refer [this](https://github.com/lightbend/config) guide for complete usage of `Config`.

Kotlin
:   @@snip [ConfigServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ConfigServiceDslExample.kts) { #motor-commands }


### Source code for examples
* [Config Service Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ConfigServiceDslExample.kts)