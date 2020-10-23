# Using the Configuration Service in Scripts

The Configuration Service (CS) is available to script writers using the provided DSL. 
The Configuration Service DSL is a wrapper over the client Configuration Service module provided by CSW.
The detailed documentation of Configuration Service provided by CSW @extref[here](csw:services/config) is useful
to understand usage of CS and limits.

The CS DSL provides methods to check if a file exists in the Configuration Service and to retrieve a file's contents.

### existsConfig

The `existsConfig` DSL method checks if provided file path exists in Configuration Service with a 
specific revision id (if provided) and returns true or false based on whether or not the file exists.

Kotlin
:   @@snip [ConfigServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ConfigServiceDslExample.kts) { #exists-config }

### getConfig

The `getConfig` DSL method retrieves the content of the file present at the provided path in Configuration Service. 
It returns `null` if file is not available in Configuration Service. 

In the below example, we are performing following steps:

1. Retrieve a configuration file from Configuration Service
1. Fail/Terminate script if configuration file does not exist
1. Parse retrieved configuration file and convert it to `MotorCommands` domain model
1. When Sequencer receives `set-motor-speed` command, then submit `set-speed` command to downstream motor HCD
1. When Sequencer receives `rotate-motor` command, then send `set-resolution` command to downstream motor HCD

Kotlin
:   @@snip [ConfigServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ConfigServiceDslExample.kts) { #get-config }

The following example shows sample code for converting a `Config` object retrieved from the Configuration Service 
to custom domain models. Note that TMT standard for configuration files is HOCON as supported by CSW.

Refer to [this](https://github.com/lightbend/config) guide for complete usage of `Config`.

Kotlin
:   @@snip [ConfigServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ConfigServiceDslExample.kts) { #motor-commands }

### Source code for examples

* [Config Service Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ConfigServiceDslExample.kts)
