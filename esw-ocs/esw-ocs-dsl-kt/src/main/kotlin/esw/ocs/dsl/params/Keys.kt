package esw.ocs.dsl.params

import csw.params.core.generics.Key
import csw.params.core.generics.KeyType
import csw.params.core.generics.Parameter
import csw.params.core.models.*
import csw.params.javadsl.JKeyType.*
import csw.params.javadsl.JUnits
import csw.time.core.models.TAITime
import csw.time.core.models.UTCTime

/** ========== Key<T> extensions ======== **/
val <T> Key<T>.keyName: String? get() = keyName()
val <T> Key<T>.keyType: KeyType<T> get() = keyType()
val <T> Key<T>.units: Units get() = units()

/** ============= Misc Keys =========== **/

fun choiceKey(name: String, units: Units, choices: Choices): Key<Choice> = ChoiceKey().make(name, units, choices)

fun choiceKey(name: String, choices: Choices): Key<Choice> = ChoiceKey().make(name, choices)

fun choiceKey(name: String, units: Units, vararg choices: Choice): Key<Choice> =
        choiceKey(name, units, Choices.fromChoices(choices.toSet()))

fun choiceKey(name: String, vararg choices: Choice): Key<Choice> = choiceKey(name, Choices.fromChoices(choices.toSet()))

fun raDecKey(name: String, units: Units = JUnits.NoUnits): Key<RaDec> = RaDecKey().make(name, units)
fun eqCoordKey(name: String, units: Units = JUnits.NoUnits): Key<Coords.EqCoord> = EqCoordKey().make(name, units)
fun solarSystemCoordKey(name: String, units: Units = JUnits.NoUnits): Key<Coords.SolarSystemCoord> = SolarSystemCoordKey().make(name, units)
fun minorPlanetCoordKey(name: String, units: Units = JUnits.NoUnits): Key<Coords.MinorPlanetCoord> = MinorPlanetCoordKey().make(name, units)
fun cometCoordKey(name: String, units: Units = JUnits.NoUnits): Key<Coords.CometCoord> = CometCoordKey().make(name, units)
fun altAzCoordKey(name: String, units: Units = JUnits.NoUnits): Key<Coords.AltAzCoord> = AltAzCoordKey().make(name, units)
fun coordKey(name: String, units: Units = JUnits.NoUnits): Key<Coords.Coord> = CoordKey().make(name, units)
fun stringKey(name: String, units: Units = JUnits.NoUnits): Key<String> = StringKey().make(name, units)
fun structKey(name: String, units: Units = JUnits.NoUnits): Key<Struct> = StructKey().make(name, units)
fun utcTimeKey(name: String): Key<UTCTime> = UTCTimeKey().make(name)
fun taiTimeKey(name: String): Key<TAITime> = TAITimeKey().make(name)

/** ============= Simple Keys =========== **/
fun booleanKey(name: String): Key<Boolean> = BooleanKey().make(name)

fun charKey(name: String, units: Units = JUnits.NoUnits): Key<Char> = CharKey().make(name, units)
fun byteKey(name: String, units: Units = JUnits.NoUnits): Key<Byte> = ByteKey().make(name, units)
fun shortKey(name: String, units: Units = JUnits.NoUnits): Key<Short> = ShortKey().make(name, units)
fun longKey(name: String, units: Units = JUnits.NoUnits): Key<Long> = LongKey().make(name, units)
fun intKey(name: String, units: Units = JUnits.NoUnits): Key<Int> = IntKey().make(name, units)
fun floatKey(name: String, units: Units = JUnits.NoUnits): Key<Float> = FloatKey().make(name, units)
fun doubleKey(name: String, units: Units = JUnits.NoUnits): Key<Double> = DoubleKey().make(name, units)

/** ============= Array Keys =========== **/
fun byteArrayKey(name: String, units: Units = JUnits.NoUnits): Key<ArrayData<Byte>> = ByteArrayKey().make(name, units)

fun shortArrayKey(name: String, units: Units = JUnits.NoUnits): Key<ArrayData<Short>> = ShortArrayKey().make(name, units)
fun longArrayKey(name: String, units: Units = JUnits.NoUnits): Key<ArrayData<Long>> = LongArrayKey().make(name, units)
fun intArrayKey(name: String, units: Units = JUnits.NoUnits): Key<ArrayData<Int>> = IntArrayKey().make(name, units)
fun floatArrayKey(name: String, units: Units = JUnits.NoUnits): Key<ArrayData<Float>> = FloatArrayKey().make(name, units)
fun doubleArrayKey(name: String, units: Units = JUnits.NoUnits): Key<ArrayData<Double>> = DoubleArrayKey().make(name, units)

/** ============= Matrix Keys =========== **/

fun byteMatrixKey(name: String, units: Units = JUnits.NoUnits): Key<MatrixData<Byte>> = ByteMatrixKey().make(name, units)

fun shortMatrixKey(name: String, units: Units = JUnits.NoUnits): Key<MatrixData<Short>> = ShortMatrixKey().make(name, units)
fun longMatrixKey(name: String, units: Units = JUnits.NoUnits): Key<MatrixData<Long>> = LongMatrixKey().make(name, units)
fun intMatrixKey(name: String, units: Units = JUnits.NoUnits): Key<MatrixData<Int>> = IntMatrixKey().make(name, units)
fun floatMatrixKey(name: String, units: Units = JUnits.NoUnits): Key<MatrixData<Float>> = FloatMatrixKey().make(name, units)
fun doubleMatrixKey(name: String, units: Units = JUnits.NoUnits): Key<MatrixData<Double>> = DoubleMatrixKey().make(name, units)

/** ============= Helpers =========== **/

inline fun <reified T> arrayData(elms: Array<T>): ArrayData<T> = ArrayData.fromArray(elms)

inline fun <reified T> arrayData(first: T, vararg rest: T): ArrayData<T> = ArrayData.fromArrays(first, *rest)

inline fun <reified T> matrixData(elms: Array<Array<T>>): MatrixData<T> = MatrixData.fromArrays(elms)
inline fun <reified T> matrixData(first: Array<T>, vararg rest: Array<T>): MatrixData<T> = MatrixData.fromArrays(first, *rest)
fun struct(vararg params: Parameter<*>): Struct = JStruct.create(*params)
fun struct(paramSet: Set<Parameter<*>>): Struct = JStruct.create(paramSet)
fun struct(params: Params): Struct = JStruct.create(params.params())

fun choicesOf(vararg choices: String): Choices = Choices.from(choices.toSet())