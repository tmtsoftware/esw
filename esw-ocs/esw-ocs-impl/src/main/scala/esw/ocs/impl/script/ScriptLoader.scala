package esw.ocs.impl.script

import scala.language.reflectiveCalls
import esw.ocs.script.host.SequenceScriptingHostKt.loadScript

import java.io.File

private[esw] object ScriptLoader {

  // XXX temp TODO FIXME: configure this somewhere
  val scriptDir = "/shared/work/tmt/csw/sequencer-scripts/scripts"

  // this loads .kts script
  def loadKotlinScript(scriptClass: String, scriptContext: ScriptContext): ScriptApi = {
    val scriptFile = new File(scriptDir, scriptClass.replace(".", "/") + ".seq.kts")
    loadScript(scriptFile) match {
      case Right(res) =>
        type Result = { def invoke(context: ScriptContext): ScriptApi }
        val result = res.asInstanceOf[Result]
        result.invoke(scriptContext)
      case Left(err) =>
        throw new RuntimeException(s"Error loading sequencer script: $scriptFile: $err")
    }
  }
}
