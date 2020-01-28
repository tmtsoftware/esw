@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.commands.Setup
import csw.params.core.generics.Key
import csw.params.core.generics.Parameter
import csw.params.javadsl.JUnits.watt
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.WFOS
import esw.ocs.dsl.params.*

script {

    val galilAssembly = Assembly(WFOS, "FilterWheel")

    onSetup("setup-wfos") {
        // #creating-params
        val temperatureKey: Key<Int> = intKey("temperature")
        val temperatureParam: Parameter<Int> = temperatureKey.set(1, 2, 3)

        // with values as Array
        val encoderKey: Key<Int> = intKey("encoder")
        val encoderValues: Array<Int> = arrayOf(1, 2, 3)
        val encoderParam: Parameter<Int> = encoderKey.set(*encoderValues)

        // with units
        val powerKey: Key<Double> = doubleKey("power")
        val values: Array<Double> = arrayOf(1.1, 2.2, 3.3)
        val powerParam: Parameter<Double> = powerKey.set(values, watt())

        // adding params to command
        val setupCommand: Setup = Setup("ESW.iris_darkNight", "move").madd(temperatureParam, encoderParam)
        // #creating-params
    }

    // #adding-params
    onSetup("setup-wfos") { command ->
        val params: Params = command.params.kMadd(intKey("temperature").set(10))
        val assemblyCommand = Setup("ESW.iris_darkNight", "move").kMadd(params)
        galilAssembly.submit(assemblyCommand)
    }
    // #adding-params

}