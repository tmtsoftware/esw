package esw.ocs.scripts.examples.aoesw

import csw.params.commands.CommandResponse
import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.*

script {
    val prefix = "aoesw.aosq"
    val aoeswOffsetTime = taiTimeKey(name = "scheduledTime")
    val aoeswOffsetXKey = floatKey("x")
    val aoeswOffsetYKey = floatKey("y")
    val probeOffsetXKey = floatKey("x")
    val probeOffsetYKey = floatKey("y")

    handleSetup("offset") { command ->
        val scheduledTime = command(aoeswOffsetTime)
        val offsetX = command(aoeswOffsetXKey)
        val offsetY = command(aoeswOffsetYKey)

        val probeOffsetXParam = probeOffsetXKey.set(offsetX(0))
        val probeOffsetYParam = probeOffsetYKey.set(offsetY(0))

        val probeCommand = setup(prefix, "scheduledOffset", command.obsId)
            .madd(probeOffsetXParam, probeOffsetYParam)

        scheduleOnce(scheduledTime(0)) {
            val probeAssembly = Assembly("probeAssembly")
            val response = probeAssembly.submitAndWait(probeCommand)
            if(response is CommandResponse.Error){
                finishWithError(response.message())
            }
        }
    }

    handleShutdown {
        println("shutdown ocs")
    }
}
