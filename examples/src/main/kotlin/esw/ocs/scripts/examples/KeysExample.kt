package esw.ocs.scripts.examples

import csw.params.events.EventName
import csw.params.events.SystemEvent
import csw.params.javadsl.JUnits
import esw.ocs.dsl.highlevel.models.Prefix
import esw.ocs.dsl.params.*

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
    val matrixParam1 = longMatrixKey("longMatrix", JUnits.lightyear).set(longMatrixData1, longMatrixData2)

    val systemEvent = SystemEvent(Prefix("ESW.event"), EventName("move")).add(longParam).madd(longParam, arrayParam, matrixParam)

    println(longParam)
    println(longKey.keyName)
    println(longKey.keyType)
    println(longKey.units)
    println(arrayParam)
    println(matrixParam)
    println(matrixParam1)
    println(systemEvent)
}
