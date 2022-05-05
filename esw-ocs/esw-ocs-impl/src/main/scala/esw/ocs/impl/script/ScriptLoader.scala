package esw.ocs.impl.script

import scala.language.reflectiveCalls
import esw.ocs.script.host.SequenceScriptingHostKt.scalaEval

import java.io.File

private[esw] object ScriptLoader {

  // XXX temp TODO FIXME: configure this somewhere
  val scriptDir = "/shared/work/tmt/csw/sequencer-scripts/scripts"

  // this loads .kts script
  def loadKotlinScript(scriptClass: String, scriptContext: ScriptContext): ScriptApi = {

    val scriptFile = new File(scriptDir, scriptClass.replace(".", "/") + ".seq.kts")
    println(s"XXX scriptFile = $scriptFile")
    // XXX FIXME
    scalaEval(scriptFile) match {
      case Right(res) =>
        println(s"XXX res = $res (${res.getClass.getName})")
        type Result = { def invoke(context: ScriptContext): ScriptApi }
        val result = res.asInstanceOf[Result]
        result.invoke(scriptContext)
      case Left(err) =>
        throw new RuntimeException(s"XXX error returned from scalaEval: $err")
    }
  }
}
