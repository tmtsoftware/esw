@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.commands.Setup
import csw.params.core.generics.Key
import csw.params.core.generics.KeyType
import csw.params.core.generics.Parameter
import csw.params.core.models.ArrayData
import csw.params.core.models.Choice
import csw.params.core.models.MatrixData
import csw.params.events.SystemEvent
import csw.params.javadsl.JKeyType
import csw.params.javadsl.JUnits
import csw.time.core.models.UTCTime
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.WFOS
import esw.ocs.dsl.params.*

script {

    val galilAssembly = Assembly(WFOS, "FilterWheel")

    onSetup("move") { command ->
        //#keys
        // Primitive keys
        val encoderKey: Key<Int> = intKey("encoder")
        val flagKey: Key<Boolean> = booleanKey("flag")
        val eventTimeKey: Key<UTCTime> = utcTimeKey("event-time")

        // Arrays
        val arrayKey: Key<ArrayData<Int>> = intArrayKey("arrayKey")
        val elms: Array<Int> = arrayOf(1, 2, 3, 4)
        val values1: ArrayData<Int> = arrayData(elms)
        val values2: ArrayData<Int> = arrayData(5, 6, 7, 8)
        val arrayParam: Parameter<ArrayData<Int>> = arrayKey.set(values1, values2)

        // Matrix
        val matrixKey: Key<MatrixData<Int>> = intMatrixKey("matrixKey")
        val arr1: Array<Int> = arrayOf(1, 2, 3, 4)
        val arr2: Array<Int> = arrayOf(5, 6, 7, 8)
        val elms1: Array<Array<Int>> = arrayOf(arr1, arr2)
        val data1: MatrixData<Int> = matrixData(elms1)
        val data2: MatrixData<Int> = matrixData(arr1, arr2)
        val matrixParameter: Parameter<MatrixData<Int>> = matrixKey.set(data1, data2)

        // Domain specific types
        val choiceKey: Key<Choice> = choiceKey("choice", choicesOf("A", "B", "C"))
        val choiceParam: Parameter<Choice> = choiceKey.set(Choice("A"), Choice("C"))

        // with units
        val temperatureInKelvinKey: Key<Int> = intKey("encoder", JUnits.kelvin)
        //#keys

    }

    onSetup("setup-wfos") { command ->
        //#creating-params
        //#getting-values
        val temperatureKey: Key<Int> = intKey("temperature")
        val temperatureParam: Parameter<Int> = temperatureKey.set(1, 2, 3)
        //#getting-values

        // with values as Array
        val encoderKey: Key<Int> = intKey("encoder")
        val encoderValues: Array<Int> = arrayOf(1, 2, 3)
        val encoderParam: Parameter<Int> = encoderKey.setAll(encoderValues)

        // with units
        val powerKey: Key<Double> = doubleKey("power")
        val values: Array<Double> = arrayOf(1.1, 2.2, 3.3)
        val powerParam: Parameter<Double> = powerKey.setAll(values).withUnits(JUnits.watt)

        // adding a param to command or event
        val setupCommand: Setup = Setup("ESW.IRIS_darkNight", "move").add(temperatureParam)
        val systemEvent: SystemEvent = SystemEvent("ESW.IRIS_darkNight", "movement").add(temperatureParam)

        // adding multiple params
        val setupCommand2: Setup = Setup("ESW.IRIS_darkNight", "move").madd(temperatureParam, encoderParam)

        // adding params of one command to other
        val paramsFromIncomingCommand: Params = command.params
        val commandForDownstream: Setup = Setup("ESW.IRIS_darkNight", "move").add(paramsFromIncomingCommand)
        // #creating-params

        //#find
        val params: Params = setupCommand.params
        val maybeParam: Parameter<Int>? = params.kFind(temperatureParam)
        val maybeParam2: Parameter<Int>? = setupCommand.kFind(temperatureParam)
        //#find

        //#getting-param-by-key
        // extracting a param from Params instance
        val temperatureParameter: Parameter<Int>? = setupCommand.params.kGet(temperatureKey)
        val temperatureParameter2: Parameter<Int> = (setupCommand.params)(temperatureKey) // alternative

        // extracting a param directly from the command or event
        val temperatureParameter3: Parameter<Int>? = setupCommand.kGet(temperatureKey)
        val temperatureParameter4: Parameter<Int> = setupCommand(temperatureKey) // alternative
        //#getting-param-by-key


        //#getting-param-by-keyName-keyType
        val keyName = "temperature"
        val keyType: KeyType<Int> = JKeyType.IntKey()
        val param: Parameter<Int>? = setupCommand.params.kGet(keyName, keyType)
        //#getting-param-by-keyName-keyType


        //#getting-values

        // extracting values from parameter
        val temperatureValues: List<Int> = temperatureParam.values

        // extracting first value from parameter
        val firstValue: Int = temperatureParam.first

        // extracting value of the parameter at a given index
        val temperatureValue: Int? = temperatureParam.kGet(1)
        val temperatureValue2: Int = temperatureParam(1) //alternative
        //#getting-values

        //#remove
        // remove param from params by key
        val updatedParams: Params = setupCommand.params.remove(temperatureKey)

        // remove param from params
        val updatedParams2: Params = setupCommand.params.remove(temperatureParameter)

        // remove param from command by key
        val updatedCommand: Setup = setupCommand.remove(temperatureKey)

        // remove param from command
        val updatedCommand2: Setup = setupCommand.remove(temperatureParameter)
        //#remove

        //#exists
        // check if parameter with specified key exists in Params
        val temperatureKeyExists: Boolean = setupCommand.params.exists(temperatureKey)

        // check if parameter with specified key exists directly from command
        val temperatureKeyExists2: Boolean = setupCommand.exists(temperatureKey)
        //#exists

    }
}