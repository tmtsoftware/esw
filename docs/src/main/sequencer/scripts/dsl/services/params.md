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

## Extracting values from a parameter

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #getting-param }  

In order to extract a specific value from items of a Parameter, you can use the following dsl which returns the value of the parameter at a given index.

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #getting-values }  

@@@note
Note that the shorthand alternatives shown with `// alternative` comment in above examples, do not return `optional` values
unlike their corresponding full version. Which means, in absence of the specified key/index, an error will occur.
@@@


## Removing a parameter

A parameter could be removed from `Params` instance or from `Command` directly. Below example demonstrates both the dsl methods.

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #remove }  

## Checking if a parameter exists

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #exists }

