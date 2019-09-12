package esw.ocs.dsl.params

import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.Key
import csw.params.core.generics.KeyType
import csw.params.core.generics.Parameter
import csw.params.core.models.*
import csw.params.events.EventName
import csw.params.events.SystemEvent
import csw.params.javadsl.JKeyType.*
import csw.params.javadsl.JUnits.NoUnits
import csw.params.javadsl.JUnits.lightyear
import csw.time.core.models.TAITime
import csw.time.core.models.UTCTime

// this is present just to hide java methods on Keys which are not user friendly
data class KeyHolder<T>(val key: Key<T>) {
    val keyName: String = key.keyName()
    val keyType: KeyType<T> = key.keyType()
    val units: Units = key.units()
    fun set(vararg elm: T, units: Units = NoUnits): Parameter<T> = key.set(elm, units)
}

fun choicesOf(vararg choices: String): Choices = Choices.from(choices.toSet())

// ============= Misc Keys ===========
fun choiceKey(name: String, choices: Choices): GChoiceKey = ChoiceKey().make(name, choices)

fun choiceKey(name: String, vararg choices: Choice): GChoiceKey =
    ChoiceKey().make(name, Choices.fromChoices(choices.toSet()))

fun raDecKey(name: String) = KeyHolder(RaDecKey().make(name))

fun eqCoordKey(name: String): KeyHolder<Coords.EqCoord> = KeyHolder(EqCoordKey().make(name))
fun solarSystemCoordKey(name: String): KeyHolder<Coords.SolarSystemCoord> = KeyHolder(SolarSystemCoordKey().make(name))
fun minorPlanetCoordKey(name: String): KeyHolder<Coords.MinorPlanetCoord> = KeyHolder(MinorPlanetCoordKey().make(name))
fun cometCoordKey(name: String): KeyHolder<Coords.CometCoord> = KeyHolder(CometCoordKey().make(name))
fun altAzCoordKey(name: String): KeyHolder<Coords.AltAzCoord> = KeyHolder(AltAzCoordKey().make(name))
fun coordKey(name: String): KeyHolder<Coords.Coord> = KeyHolder(CoordKey().make(name))
fun stringKey(name: String): KeyHolder<String> = KeyHolder(StringKey().make(name))
fun structKey(name: String): KeyHolder<Struct> = KeyHolder(StructKey().make(name))
fun utcTimeKey(name: String): KeyHolder<UTCTime> = KeyHolder(UTCTimeKey().make(name))
fun taiTimeKey(name: String): KeyHolder<TAITime> = KeyHolder(TAITimeKey().make(name))

// ============= Simple Keys ===========
fun booleanKey(name: String): KeyHolder<Boolean> = KeyHolder(BooleanKey().make(name))

fun charKey(name: String): KeyHolder<Char> = KeyHolder(CharKey().make(name))
fun byteKey(name: String): KeyHolder<Byte> = KeyHolder(ByteKey().make(name))
fun shortKey(name: String): KeyHolder<Short> = KeyHolder(ShortKey().make(name))
fun longKey(name: String): KeyHolder<Long> = KeyHolder(LongKey().make(name))
fun intKey(name: String): KeyHolder<Int> = KeyHolder(IntKey().make(name))
fun floatKey(name: String): KeyHolder<Float> = KeyHolder(FloatKey().make(name))
fun doubleKey(name: String): KeyHolder<Double> = KeyHolder(DoubleKey().make(name))

// ============= Array Keys ===========
fun byteArrayKey(name: String): KeyHolder<ArrayData<Byte>> = KeyHolder(ByteArrayKey().make(name))

fun shortArrayKey(name: String): KeyHolder<ArrayData<Short>> = KeyHolder(ShortArrayKey().make(name))
fun longArrayKey(name: String): KeyHolder<ArrayData<Long>> = KeyHolder(LongArrayKey().make(name))
fun intArrayKey(name: String): KeyHolder<ArrayData<Int>> = KeyHolder(IntArrayKey().make(name))
fun floatArrayKey(name: String): KeyHolder<ArrayData<Float>> = KeyHolder(FloatArrayKey().make(name))
fun doubleArrayKey(name: String): KeyHolder<ArrayData<Double>> = KeyHolder(DoubleArrayKey().make(name))

// ============= Matrix Keys ===========
fun byteMatrixKey(name: String): KeyHolder<MatrixData<Byte>> = KeyHolder(ByteMatrixKey().make(name))

fun shortMatrixKey(name: String): KeyHolder<MatrixData<Short>> = KeyHolder(ShortMatrixKey().make(name))
fun longMatrixKey(name: String): KeyHolder<MatrixData<Long>> = KeyHolder(LongMatrixKey().make(name))
fun intMatrixKey(name: String): KeyHolder<MatrixData<Int>> = KeyHolder(IntMatrixKey().make(name))
fun floatMatrixKey(name: String): KeyHolder<MatrixData<Float>> = KeyHolder(FloatMatrixKey().make(name))
fun doubleMatrixKey(name: String): KeyHolder<MatrixData<Double>> = KeyHolder(DoubleMatrixKey().make(name))

fun <T> arrayData(elms: Array<T>): ArrayData<T> = ArrayData.fromJavaArray(elms)
inline fun <reified T> matrixData(elms: Array<Array<T>>): MatrixData<T> = MatrixData.fromJavaArrays(T::class.java, elms)

// =================================================
// ================= Sample Usage ==================
// =================================================
fun main() {

    // ======== SimpleKey ==========
    val longKey = longKey("long")
    val longParam = longKey.set(10, 20, 30)

    // ======== ArrayKey ==========
    val data1 = arrayData(arrayOf(100L, 200L, 300L))
    val data2 = arrayData(arrayOf(400L, 500L, 600L, 700L))
    val arrayParam = longArrayKey("longArray").set(data1, data2)

    // ======== MatrixKey ==========
    val longData1 = arrayOf(arrayOf(100L, 200L, 300L), arrayOf(400L, 500L, 600L))
    val longData2 = arrayOf(arrayOf(500L, 600L, 700L), arrayOf(800L, 900L, 1000L))
    val longMatrixData1 = matrixData(longData1)
    val longMatrixData2 = matrixData(longData2)
    val matrixParam = longMatrixKey("longMatrix").set(longMatrixData1, longMatrixData2)
    val matrixParam1 = longMatrixKey("longMatrix").set(longMatrixData1, longMatrixData2, units = lightyear)

    val systemEvent =
        SystemEvent(Prefix("esw.event"), EventName("move")).add(longParam)
            .madd(longParam, arrayParam, matrixParam)

    println(longParam)
    println(longKey.keyName)
    println(longKey.keyType)
    println(longKey.units)
    println(arrayParam)
    println(matrixParam)
    println(matrixParam1)
    println(systemEvent)
}
