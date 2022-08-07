package esw.ocs.impl.script

import esw.ocs.impl.script.exceptions.ScriptInitialisationFailedException
import esw.ocs.impl.script.exceptions.ScriptLoadingException.{InvalidScriptException, ScriptNotFound}

import scala.language.reflectiveCalls
import esw.ocs.script.ScriptDefKt.loadScript

import java.io.File

private[esw] object ScriptLoader {

  // noinspection ScalaUnusedSymbol
  private def invokeScript(scriptContext: ScriptContext, res: AnyRef): ScriptApi = {
    type Result = { def invoke(context: ScriptContext): ScriptApi }
    val result = res.asInstanceOf[Result]
    result.invoke(scriptContext)
  }

  // this loads .kts script
  def loadKotlinScript(scriptFile: File, scriptContext: ScriptContext): ScriptApi = {
    if (!scriptFile.isFile)
      throw new ScriptNotFound(scriptFile.getPath)
    loadScript(scriptFile) match {
      case Right(res) =>
        invokeScript(scriptContext, res)
      case Left(err) =>
        throw new InvalidScriptException(scriptFile.getPath)
    }
  }
}
