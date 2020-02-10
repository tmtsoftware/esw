# Params

A DSL has been provided for creating Parameters to store values like primitive types, collection types or domain specific types,
to be used for `ParameterSets` in Events and Commands.
This DSL is built over abstractions like `Parameter`, `KeyType`, etc. offered by CSW.
Refer to the @extref[CSW doc](csw:params/keys-parameters) for more information about this. 

## Keys

Following table lists all the key types, and their corresponding DSL provided.

| KeyType             | DSL                  |
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

The example below shows usages of the DSL for different types of keys. Some other DSL helpers like `struct`, `choicesOf`, `arrayData`, `matrixData`, etc.
have also been provided for ease of access. Usage of these helper DSLs is also shown in the example below.

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #keys }  


## Creating Parameters

The example below shows different ways of creating parameters and adding them to a command.

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

The example below shows the accessing of values of a parameter, or accessing a specific value of a parameter.

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #getting-values }  

@@@note
Note that the shorthand alternatives shown with `// alternative` comment in above examples, do not return `optional` values
unlike their corresponding full version. This means, with shorthand DSL, an error will occur in the absence of the specified key/index.
@@@


## Removing a parameter

A parameter could be removed from a `Params` instance or from a `Command` directly. The example below demonstrates both of these DSL methods.

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #remove }  

## Checking if a parameter exists

Kotlin
: @@snip [Params.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/ParamsExample.kts) { #exists }

