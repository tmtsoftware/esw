# Params

A Kotlin Dsl has been provided for creating Parameters to store values like primitive types, collection types or domain specific types.
This Dsl is built over abstractions like `Parameter`, `KeyType` etc offered by CSW.
Refer to the @extref[CSW doc](csw:params/keys-parameters) for more information about this. 

## Creating Parameters

Example below shows different ways of creating parameters and adding them to a command.

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #creating-params }  


Below examples demonstrates extracting params from received command, adding an additional parameter and
creating a new command with the new set of params.

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #adding-params }  
