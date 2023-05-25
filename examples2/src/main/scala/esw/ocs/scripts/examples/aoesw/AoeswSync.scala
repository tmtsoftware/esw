package esw.ocs.scripts.examples.aoesw

import csw.params.commands.CommandResponse
import csw.params.core.generics.KeyType.{FloatKey, TAITimeKey}
import esw.ocs.dsl2.core.Script
import esw.ocs.impl.script.ScriptContext

import scala.concurrent.duration.DurationInt

class AoeswSync(scriptContext: ScriptContext) extends Script(scriptContext):
  val prefix          = "AOESW.aosq"
  val aoeswOffsetTime = TAITimeKey.make(name = "scheduledTime")
  val aoeswOffsetXKey = FloatKey.make("x")
  val aoeswOffsetYKey = FloatKey.make("y")
  val probeOffsetXKey = FloatKey.make("x")
  val probeOffsetYKey = FloatKey.make("y")

  onSetup("offset") { command =>
    val scheduledTime = command(aoeswOffsetTime)
    val offsetX       = command(aoeswOffsetXKey)
    val offsetY       = command(aoeswOffsetYKey)

    val probeOffsetXParam = probeOffsetXKey.set(offsetX(0))
    val probeOffsetYParam = probeOffsetYKey.set(offsetY(0))

    val probeCommand = Setup(prefix, "scheduledOffset", command.obsId)
      .madd(probeOffsetXParam, probeOffsetYParam)

    scheduleOnce(scheduledTime(0)) {
      val probeAssembly = Assembly(ESW, "probeAssembly", 10.seconds)
      val response      = probeAssembly.submitAndWait(probeCommand, 10.seconds)
      if (response == CommandResponse.Error) {
        // asdasdasd
        finishWithError(response.toString)
      }
    }
    ()
  }

  onShutdown {
    println("shutdown ocs")
  }
