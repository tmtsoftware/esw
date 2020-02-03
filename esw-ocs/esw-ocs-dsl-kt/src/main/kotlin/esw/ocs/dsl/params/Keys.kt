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
fun <T> Key<T>.set(vararg elm: T, units: Units = JUnits.NoUnits()): Parameter<T> = set(elm, units)

/** ============= Misc Keys =========== **/
fun choiceKey(name: String, choices: Choices): Key<Choice> = ChoiceKey().make(name, choices)

fun choiceKey(name: String, vararg choices: Choice): Key<Choice> =
        ChoiceKey().make(name, Choices.fromChoices(choices.toSet()))

fun raDecKey(name: String): Key<RaDec> = RaDecKey().make(name)
fun eqCoordKey(name: String): Key<Coords.EqCoord> = EqCoordKey().make(name)
fun solarSystemCoordKey(name: String): Key<Coords.SolarSystemCoord> = SolarSystemCoordKey().make(name)
fun minorPlanetCoordKey(name: String): Key<Coords.MinorPlanetCoord> = MinorPlanetCoordKey().make(name)
fun cometCoordKey(name: String): Key<Coords.CometCoord> = CometCoordKey().make(name)
fun altAzCoordKey(name: String): Key<Coords.AltAzCoord> = AltAzCoordKey().make(name)
fun coordKey(name: String): Key<Coords.Coord> = CoordKey().make(name)
fun stringKey(name: String): Key<String> = StringKey().make(name)
fun structKey(name: String): Key<Struct> = StructKey().make(name)
fun utcTimeKey(name: String): Key<UTCTime> = UTCTimeKey().make(name)
fun taiTimeKey(name: String): Key<TAITime> = TAITimeKey().make(name)

/** ============= Simple Keys =========== **/
fun booleanKey(name: String): Key<Boolean> = BooleanKey().make(name)

fun charKey(name: String): Key<Char> = CharKey().make(name)
fun byteKey(name: String): Key<Byte> = ByteKey().make(name)
fun shortKey(name: String): Key<Short> = ShortKey().make(name)
fun longKey(name: String): Key<Long> = LongKey().make(name)
fun intKey(name: String): Key<Int> = IntKey().make(name)
fun floatKey(name: String): Key<Float> = FloatKey().make(name)
fun doubleKey(name: String): Key<Double> = DoubleKey().make(name)

/** ============= Array Keys =========== **/
fun byteArrayKey(name: String): Key<ArrayData<Byte>> = ByteArrayKey().make(name)

fun shortArrayKey(name: String): Key<ArrayData<Short>> = ShortArrayKey().make(name)
fun longArrayKey(name: String): Key<ArrayData<Long>> = LongArrayKey().make(name)
fun intArrayKey(name: String): Key<ArrayData<Int>> = IntArrayKey().make(name)
fun floatArrayKey(name: String): Key<ArrayData<Float>> = FloatArrayKey().make(name)
fun doubleArrayKey(name: String): Key<ArrayData<Double>> = DoubleArrayKey().make(name)

/** ============= Matrix Keys =========== **/

fun byteMatrixKey(name: String): Key<MatrixData<Byte>> = ByteMatrixKey().make(name)

fun shortMatrixKey(name: String): Key<MatrixData<Short>> = ShortMatrixKey().make(name)
fun longMatrixKey(name: String): Key<MatrixData<Long>> = LongMatrixKey().make(name)
fun intMatrixKey(name: String): Key<MatrixData<Int>> = IntMatrixKey().make(name)
fun floatMatrixKey(name: String): Key<MatrixData<Float>> = FloatMatrixKey().make(name)
fun doubleMatrixKey(name: String): Key<MatrixData<Double>> = DoubleMatrixKey().make(name)

/** ============= Helpers =========== **/

inline fun <reified T> arrayData(elms: Array<T>): ArrayData<T> = ArrayData.fromArray(elms)

inline fun <reified T> arrayData(first: T, vararg rest: T): ArrayData<T> = ArrayData.fromArrays(first, *rest)

inline fun <reified T> matrixData(elms: Array<Array<T>>): MatrixData<T> = MatrixData.fromArrays(elms)
inline fun <reified T> matrixData(first: Array<T>, vararg rest: Array<T>): MatrixData<T> = MatrixData.fromArrays(first, *rest)
fun struct(vararg params: Parameter<*>): Struct = JStruct.create(*params)
fun struct(paramSet: Set<Parameter<*>>): Struct = JStruct.create(paramSet)
fun struct(params: Params): Struct = JStruct.create(params.params())

fun choicesOf(vararg choices: String): Choices = Choices.from(choices.toSet())
