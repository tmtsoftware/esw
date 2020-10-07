package esw.ocs.dsl.params

import csw.params.core.models.ArrayData
import csw.params.core.models.Choice
import csw.params.javadsl.JKeyType.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class KeysTest {

    @Test
    fun `verify kotlin to scala key mappings`() {
        choiceKey("choiceKey", choicesOf("A", "B")) shouldBe ChoiceKey().make("choiceKey", choicesOf("A", "B"))
        choiceKey("choiceKey", Choice("A")) shouldBe ChoiceKey().make("choiceKey", choicesOf("A"))
        raDecKey("raDecKey") shouldBe RaDecKey().make("raDecKey")
        eqCoordKey("eqCoordKey") shouldBe EqCoordKey().make("eqCoordKey")
        solarSystemCoordKey("solarSystemCoordKey") shouldBe SolarSystemCoordKey().make("solarSystemCoordKey")
        minorPlanetCoordKey("minorPlanetCoordKey") shouldBe MinorPlanetCoordKey().make("minorPlanetCoordKey")
        cometCoordKey("cometCoordKey") shouldBe CometCoordKey().make("cometCoordKey")
        altAzCoordKey("altAzCoordKey") shouldBe AltAzCoordKey().make("altAzCoordKey")
        coordKey("coordKey") shouldBe CoordKey().make("coordKey")
        stringKey("stringKey") shouldBe StringKey().make("stringKey")
        structKey("structKey") shouldBe StructKey().make("structKey")
        utcTimeKey("utcTimeKey") shouldBe UTCTimeKey().make("utcTimeKey")
        taiTimeKey("taiTimeKey") shouldBe TAITimeKey().make("taiTimeKey")
        booleanKey("booleanKey") shouldBe BooleanKey().make("booleanKey")
        charKey("charKey") shouldBe CharKey().make("charKey")
        byteKey("byteKey") shouldBe ByteKey().make("byteKey")
        shortKey("shortKey") shouldBe ShortKey().make("shortKey")
        longKey("longKey") shouldBe LongKey().make("longKey")
        intKey("intKey") shouldBe IntKey().make("intKey")
        floatKey("floatKey") shouldBe FloatKey().make("floatKey")
        doubleKey("doubleKey") shouldBe DoubleKey().make("doubleKey")
        byteArrayKey("byteArrayKey") shouldBe ByteArrayKey().make("byteArrayKey")
        shortArrayKey("shortArrayKey") shouldBe ShortArrayKey().make("shortArrayKey")
        longArrayKey("longArrayKey") shouldBe LongArrayKey().make("longArrayKey")
        intArrayKey("intArrayKey") shouldBe IntArrayKey().make("intArrayKey")
        floatArrayKey("floatArrayKey") shouldBe FloatArrayKey().make("floatArrayKey")
        doubleArrayKey("doubleArrayKey") shouldBe DoubleArrayKey().make("doubleArrayKey")
        byteMatrixKey("byteMatrixKey") shouldBe ByteMatrixKey().make("byteMatrixKey")
        shortMatrixKey("shortMatrixKey") shouldBe ShortMatrixKey().make("shortMatrixKey")
        longMatrixKey("longMatrixKey") shouldBe LongMatrixKey().make("longMatrixKey")
        intMatrixKey("intMatrixKey") shouldBe IntMatrixKey().make("intMatrixKey")
        floatMatrixKey("floatMatrixKey") shouldBe FloatMatrixKey().make("floatMatrixKey")
        doubleMatrixKey("doubleMatrixKey") shouldBe DoubleMatrixKey().make("doubleMatrixKey")
    }

    @Test
    fun `verify kotlin to scala arrayData and matrixData mappings`() {
        val arrayData = arrayData(arrayOf(1, 2, 3))
        arrayData.shouldBeTypeOf<ArrayData<Int>>()
        arrayData.jValues() shouldBe listOf(1, 2, 3)

        val matrixData = matrixData(arrayOf(arrayOf(1, 2), arrayOf(3, 4)))
        //fixme: why does this not work
        //matrixData.shouldBeTypeOf<MatrixData<Int>>()
        matrixData.jValues() shouldBe listOf(listOf(1, 2), listOf(3, 4))
    }

}
