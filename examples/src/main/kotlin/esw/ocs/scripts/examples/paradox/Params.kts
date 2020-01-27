@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.WFOS
import esw.ocs.dsl.params.intKey
import esw.ocs.dsl.params.kMadd
import esw.ocs.dsl.params.params

script {

    val galilAssembly = Assembly(WFOS, "FilterWheel")


    // #adding-params
    onSetup("setup-wfos") { command ->
        val params = command.params.kMadd(intKey("temperature").set(10))
        val assemblyCommand = Setup("ESW.IRIS_darkNight", "move").kMadd(params)
        galilAssembly.submit(assemblyCommand)
    }
    // #adding-params

}