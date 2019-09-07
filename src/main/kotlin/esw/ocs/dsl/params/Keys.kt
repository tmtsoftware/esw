package esw.ocs.dsl.params

import csw.params.core.generics.Key
import csw.params.core.generics.KeyType
import csw.params.core.generics.Parameter
import csw.params.core.models.*
import csw.params.events.EventName
import csw.params.events.SystemEvent
import csw.params.javadsl.JKeyType.*
import csw.params.javadsl.JUnits.NoUnits

// this is present just to hide java methods on Keys which are not user friendly
data class KeyHolder<T>(val key: Key<T>) {
    val keyName: String = key.keyName()
    val keyType: KeyType<T> = key.keyType()
    val units: Units = key.units()
    fun set(vararg elm: T, units: Units = NoUnits): Parameter<T> = key.set(elm, units)
}

// ============= Misc Keys ===========
fun raDecKey(name: String) = KeyHolder(RaDecKey().make(name))

fun eqCoordKey(name: String) = KeyHolder(EqCoordKey().make(name))
fun solarSystemCoordKey(name: String) = KeyHolder(SolarSystemCoordKey().make(name))
fun minorPlanetCoordKey(name: String) = KeyHolder(MinorPlanetCoordKey().make(name))
fun cometCoordKey(name: String) = KeyHolder(CometCoordKey().make(name))
fun altAzCoordKey(name: String) = KeyHolder(AltAzCoordKey().make(name))
fun coordKey(name: String) = KeyHolder(CoordKey().make(name))
fun stringKey(name: String) = KeyHolder(StringKey().make(name))
fun structKey(name: String) = KeyHolder(StructKey().make(name))
fun utcTimeKey(name: String) = KeyHolder(UTCTimeKey().make(name))
fun taiTimeKey(name: String) = KeyHolder(TAITimeKey().make(name))

// ============= Simple Keys ===========
fun booleanKey(name: String) = KeyHolder(BooleanKey().make(name))

fun charKey(name: String) = KeyHolder(CharKey().make(name))
fun byteKey(name: String) = KeyHolder(ByteKey().make(name))
fun shortKey(name: String) = KeyHolder(ShortKey().make(name))
fun longKey(name: String) = KeyHolder(LongKey().make(name))
fun intKey(name: String) = KeyHolder(IntKey().make(name))
fun floatKey(name: String) = KeyHolder(FloatKey().make(name))
fun doubleKey(name: String) = KeyHolder(DoubleKey().make(name))

// ============= Array Keys ===========
fun byteArrayKey(name: String) = KeyHolder(ByteArrayKey().make(name))

fun shortArrayKey(name: String) = KeyHolder(ShortArrayKey().make(name))
fun longArrayKey(name: String) = KeyHolder(LongArrayKey().make(name))
fun intArrayKey(name: String) = KeyHolder(IntArrayKey().make(name))
fun floatArrayKey(name: String) = KeyHolder(FloatArrayKey().make(name))
fun doubleArrayKey(name: String) = KeyHolder(DoubleArrayKey().make(name))

// ============= Matrix Keys ===========
fun byteMatrixKey(name: String) = KeyHolder(ByteMatrixKey().make(name))

fun shortMatrixKey(name: String) = KeyHolder(ShortMatrixKey().make(name))
fun longMatrixKey(name: String) = KeyHolder(LongMatrixKey().make(name))
fun intMatrixKey(name: String) = KeyHolder(IntMatrixKey().make(name))
fun floatMatrixKey(name: String) = KeyHolder(FloatMatrixKey().make(name))
fun doubleMatrixKey(name: String) = KeyHolder(DoubleMatrixKey().make(name))

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
//    val matrixParam1 = longMatrixKey("longMatrix").set(arrayOf(longMatrixData1, longMatrixData2), NoUnits)

    val systemEvent = SystemEvent(Prefix("esw.event"), EventName("move")).add(longParam)
//    systemEvent.madd()

    println(longParam)
    println(longKey.keyName)
    println(longKey.keyType)
    println(longKey.units)
    println(arrayParam)
    println(matrixParam)
    println(systemEvent)
}