# Params

A Kotlin Dsl has been provided for creating Parameters to store values like primitive types, collection types or domain specific types.
This Dsl is built over abstractions like `Parameter`, `KeyType` etc offered by CSW.
Refer to the @extref[CSW doc](csw:params/keys-parameters) for more information about this. 

## Keys

Following table lists all the key types, and their corresponding kotlin dsl provided.

| KeyType             | Kotlin Dsl                  |
| :-----------------: |:--------------------------: |
| Boolean             | booleanKey                  |
| Character           | charKey                     |
| Byte                | byteKey                     |
| Short               | shortKey                    |
| Long                | longKey                     |
| Int                 | intKey                      |
| Float               | floatKey                    |
| Double              | doubleKey                   |
| String              | stringKey                   |
| UtcTime             | utcTimeKey                  |
| TaiTime             | taiTimeKey                  |
| ByteArray           | byteArrayKey                |
| ShortArray          | shortArrayKey               |
| LongArray           | longArrayKey                |
| IntArray            | intArrayKey                 |
| FloatArray          | floatArrayKey               |
| DoubleArray         | doubleArrayKey              |
| ByteMatrix          | byteMatrixKey               |
| ShortMatrix         | shortMatrixKey              |
| LongMatrix          | longMatrixKey               |
| IntMatrix           | intMatrixKey                |
| FloatMatrix         | floatMatrixKey              |
| DoubleMatrix        | doubleMatrixKey             |
| Choice              | choiceKey                   |
| Struct              | structKey                   |
| RaDec               | raDecKey                    |
| EqCoord             | eqCoordKey                  |
| SolarSystemCoord    | solarSystemCoordKey         |
| MinorPlanetCoord    | minorPlanetCoordKey         |
| CometCoord          | cometCoordKey               |
| AltAzCoord          | altAzCoordKey               |
| Coord  (*)          | coordKey                    |

Example below shows usages of Dsl for different types of keys. Some other helper Dsl like `struct`, `choicesOf`, `arrayData`, `matrixData` etc
have also been provided for ease of access. Usage of this helper Dsl is also shown in the below example.

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #keys }  


## Creating Parameters

Example below shows different ways of creating parameters and adding them to a command.

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #creating-params }  

## Extracting a parameter from Params/Command/Event

### Extracting parameter

Finding a parameter from Params/Command/Event.

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #find }  

### Extracting parameter by key

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #getting-param-by-key }  

### Extracting parameter by keyName and KeyType

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #getting-param-by-keyName-keyType }  

## Extracting values from a parameter

Example below shows accessing values of a parameter, or accessing a specific value of a parameter.

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #getting-values }  

@@@note
Note that the shorthand alternatives shown with `// alternative` comment in above examples, do not return `optional` values
unlike their corresponding full version. Which means, with shorthand dsl, an error will occur in absence of the specified key/index.
@@@


## Removing a parameter

A parameter could be removed from `Params` instance or from `Command` directly. Below example demonstrates both the dsl methods.

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #remove }  

## Checking if a parameter exists

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #exists }

