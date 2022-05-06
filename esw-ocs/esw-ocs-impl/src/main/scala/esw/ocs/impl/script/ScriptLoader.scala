package esw.ocs.impl.script

import scala.language.reflectiveCalls
import esw.ocs.script.host.SequenceScriptingHostKt.loadScript

import java.io.File

private[esw] object ScriptLoader {

  // We need to know where the sequencer-scripts repo is in order to load the scripts at runtime.
  // For now, require that the environment variable is set.
  private val maybeSequencerScriptsDir = sys.env.get("SEQUENCER_SCRIPTS_HOME")
  if (maybeSequencerScriptsDir.isEmpty)
    throw new RuntimeException(
      "Please set the SEQUENCER_SCRIPTS_HOME environment variable to the root of the checked out sequencer-scripts repo"
    )
  private val scriptDir = s"${maybeSequencerScriptsDir.get}/scripts"

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
