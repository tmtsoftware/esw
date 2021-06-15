package esw.ocs.scripts.examples.aoesw

import csw.params.commands.CommandResponse
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.ESW
import esw.ocs.dsl.params.floatKey
import esw.ocs.dsl.params.invoke
import esw.ocs.dsl.params.taiTimeKey
import kotlin.time.Duration

script {
    val prefix = "AOESW.aosq"
    val aoeswOffsetTime = taiTimeKey(name = "scheduledTime")
    val aoeswOffsetXKey = floatKey("x")
    val aoeswOffsetYKey = floatKey("y")
    val probeOffsetXKey = floatKey("x")
    val probeOffsetYKey = floatKey("y")

    onSetup("offset") { command ->
        val scheduledTime = command(aoeswOffsetTime)
        val offsetX = command(aoeswOffsetXKey)
        val offsetY = command(aoeswOffsetYKey)

        val probeOffsetXParam = probeOffsetXKey.set(offsetX(0))
        val probeOffsetYParam = probeOffsetYKey.set(offsetY(0))

        val probeCommand = Setup(prefix, "scheduledOffset", command.obsId)
            .madd(probeOffsetXParam, probeOffsetYParam)

        scheduleOnce(scheduledTime(0)) {
            val probeAssembly = Assembly(ESW, "probeAssembly", Duration.seconds(10))
            val response = probeAssembly.submitAndWait(probeCommand, Duration.seconds(10))
            if(response is CommandResponse.Error){
                finishWithError(response.message())
            }
        }
    }

    onShutdown {
        println("shutdown ocs")
    }
}
