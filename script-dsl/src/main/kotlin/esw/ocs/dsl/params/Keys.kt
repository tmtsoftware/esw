package esw.ocs.dsl.params

import csw.params.core.models.*
import csw.params.javadsl.JKeyType.*
import csw.time.core.models.TAITime
import csw.time.core.models.UTCTime

fun choicesOf(vararg choices: String): Choices = Choices.from(choices.toSet())

// ============= Misc Keys ===========
fun choiceKey(name: String, choices: Choices): KeyKt<Choice> = KeyKt(ChoiceKey().make(name, choices))

fun choiceKey(name: String, vararg choices: Choice): KeyKt<Choice> =
    KeyKt(ChoiceKey().make(name, Choices.fromChoices(choices.toSet())))

fun raDecKey(name: String): KeyKt<RaDec> = KeyKt(RaDecKey().make(name))
fun eqCoordKey(name: String): KeyKt<Coords.EqCoord> = KeyKt(EqCoordKey().make(name))
fun solarSystemCoordKey(name: String): KeyKt<Coords.SolarSystemCoord> = KeyKt(SolarSystemCoordKey().make(name))
fun minorPlanetCoordKey(name: String): KeyKt<Coords.MinorPlanetCoord> = KeyKt(MinorPlanetCoordKey().make(name))
fun cometCoordKey(name: String): KeyKt<Coords.CometCoord> = KeyKt(CometCoordKey().make(name))
fun altAzCoordKey(name: String): KeyKt<Coords.AltAzCoord> = KeyKt(AltAzCoordKey().make(name))
fun coordKey(name: String): KeyKt<Coords.Coord> = KeyKt(CoordKey().make(name))
fun stringKey(name: String): KeyKt<String> = KeyKt(StringKey().make(name))
fun structKey(name: String): KeyKt<Struct> = KeyKt(StructKey().make(name))
fun utcTimeKey(name: String): KeyKt<UTCTime> = KeyKt(UTCTimeKey().make(name))
fun taiTimeKey(name: String): KeyKt<TAITime> = KeyKt(TAITimeKey().make(name))

// ============= Simple Keys ===========
fun booleanKey(name: String): KeyKt<Boolean> = KeyKt(BooleanKey().make(name))

fun charKey(name: String): KeyKt<Char> = KeyKt(CharKey().make(name))
fun byteKey(name: String): KeyKt<Byte> = KeyKt(ByteKey().make(name))
fun shortKey(name: String): KeyKt<Short> = KeyKt(ShortKey().make(name))
fun longKey(name: String): KeyKt<Long> = KeyKt(LongKey().make(name))
fun intKey(name: String): KeyKt<Int> = KeyKt(IntKey().make(name))
fun floatKey(name: String): KeyKt<Float> = KeyKt(FloatKey().make(name))
fun doubleKey(name: String): KeyKt<Double> = KeyKt(DoubleKey().make(name))

// ============= Array Keys ===========
fun byteArrayKey(name: String): KeyKt<ArrayData<Byte>> = KeyKt(ByteArrayKey().make(name))

fun shortArrayKey(name: String): KeyKt<ArrayData<Short>> = KeyKt(ShortArrayKey().make(name))
fun longArrayKey(name: String): KeyKt<ArrayData<Long>> = KeyKt(LongArrayKey().make(name))
fun intArrayKey(name: String): KeyKt<ArrayData<Int>> = KeyKt(IntArrayKey().make(name))
fun floatArrayKey(name: String): KeyKt<ArrayData<Float>> = KeyKt(FloatArrayKey().make(name))
fun doubleArrayKey(name: String): KeyKt<ArrayData<Double>> = KeyKt(DoubleArrayKey().make(name))

// ============= Matrix Keys ===========
fun byteMatrixKey(name: String): KeyKt<MatrixData<Byte>> = KeyKt(ByteMatrixKey().make(name))

fun shortMatrixKey(name: String): KeyKt<MatrixData<Short>> = KeyKt(ShortMatrixKey().make(name))
fun longMatrixKey(name: String): KeyKt<MatrixData<Long>> = KeyKt(LongMatrixKey().make(name))
fun intMatrixKey(name: String): KeyKt<MatrixData<Int>> = KeyKt(IntMatrixKey().make(name))
fun floatMatrixKey(name: String): KeyKt<MatrixData<Float>> = KeyKt(FloatMatrixKey().make(name))
fun doubleMatrixKey(name: String): KeyKt<MatrixData<Double>> = KeyKt(DoubleMatrixKey().make(name))

fun <T> arrayData(elms: Array<T>): ArrayData<T> = ArrayData.fromJavaArray(elms)
inline fun <reified T> matrixData(elms: Array<Array<T>>): MatrixData<T> = MatrixData.fromJavaArrays(T::class.java, elms)
