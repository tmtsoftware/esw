package esw.performance.scripts

import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.CSW
import kotlin.time.seconds

script {

    println("Loaded script successfully from examples!!!!")
    val testAssembly = Assembly(CSW, "sampleAssembly", 60.seconds)
    println("test assembly resolved successfully !!!!")

    onSetup("command-2") { command ->
        val command3 = Setup(command.source().toString(), "command-3")
        println("Submitted to test assembly")
        val submitResponse = testAssembly.submitAndWait(command3, 60.seconds)
        println("Submit response: $submitResponse")
    }
}