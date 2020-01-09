# Config Service

Config Service Dsl is a wrapper over config service module provided by csw. 
This dsl provides APIs to check if file exists in configuration service and retrieve its contents.

## existsConfig

This DSL checks if provided file exists in configuration service with given revision id (if provided) and returns true or false based on that.

Kotlin
:   @@snip [ConfigServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ConfigServiceDslExample.kts) { #exists-config }

## getConfig

This DSL retrieves the content of the file present at the provided path in configuration service. 
It returns `null` if file is not available in configuration service. 

Kotlin
:   @@snip [ConfigServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ConfigServiceDslExample.kts) { #get-config }

### Source code for examples
* @github[Config Service Examples](/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ConfigServiceDslExample.kts)