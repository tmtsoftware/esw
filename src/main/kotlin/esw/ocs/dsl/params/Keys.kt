package esw.ocs.dsl.params

import csw.params.core.generics.Key
import csw.params.core.generics.KeyType
import csw.params.core.generics.Parameter
import csw.params.core.models.ArrayData
import csw.params.core.models.MatrixData
import csw.params.core.models.Prefix
import csw.params.core.models.Units
import csw.params.events.EventName
import csw.params.events.SystemEvent
import csw.params.javadsl.JKeyType.*
import csw.params.javadsl.JUnits.NoUnits

data class KeyHolder<T>(val key: Key<T>) {
    val keyType: KeyType<T> = key.keyType()
    val keyName: String = key.keyName()
    fun add(vararg elm: T, units: Units = NoUnits): Parameter<T> = key.set(elm, units)
}

fun longKey(name: String): KeyHolder<Long> = KeyHolder(LongKey().make(name))
fun longArrayKey(name: String): KeyHolder<ArrayData<Long>> = KeyHolder(LongArrayKey().make(name))
fun longMatrixKey(name: String): KeyHolder<MatrixData<Long>> = KeyHolder(LongMatrixKey().make(name))

fun <T> arrayData(elms: Array<T>): ArrayData<T> = ArrayData.fromJavaArray(elms)
inline fun <reified T> matrixData(elms: Array<Array<T>>): MatrixData<T> = MatrixData.fromJavaArrays(T::class.java, elms)

fun main() {

    // ======== SimpleKey ==========
    val longKey = longKey("long")
    val longParam = longKey.add(10, 20, 30)
    longKey.keyName
    longKey.keyType

    // ======== ArrayKey ==========
    val data1 = arrayData(arrayOf(100L, 200L, 300L))
    val data2 = arrayData(arrayOf(400L, 500L, 600L, 700L))
    val arrayParam = longArrayKey("longArray").add(data1, data2)

    // ======== MatrixKey ==========
    val longData1 = arrayOf(arrayOf(100L, 200L, 300L), arrayOf(400L, 500L, 600L))
    val longData2 = arrayOf(arrayOf(500L, 600L, 700L), arrayOf(800L, 900L, 1000L))
    val longMatrixData1 = matrixData(longData1)
    val longMatrixData2 = matrixData(longData2)
    val matrixParam = longMatrixKey("longMatrix").add(longMatrixData1, longMatrixData2)
//    val matrixParam1 = longMatrixKey("longMatrix").set(arrayOf(longMatrixData1, longMatrixData2), NoUnits)

    println(longParam)
    println(arrayParam)
    println(matrixParam)


    val systemEvent = SystemEvent(Prefix("event"), EventName("move")).add(longParam)

//    systemEvent.madd()
}