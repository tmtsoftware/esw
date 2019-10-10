package esw.ocs.dsl.script.utils

import esw.ocs.dsl.script.exceptions.ScriptLoadingException.{InvalidScriptException, ScriptNotFound}
import esw.ocs.dsl.script.{CswServices, ScriptDsl}

import scala.language.reflectiveCalls

private[esw] object ScriptLoader {

  // this loads .kts script
  def loadKotlinScript(scriptClass: String, cswServices: CswServices): ScriptDsl =
    withScript(scriptClass) { clazz =>
      val script = clazz.getConstructor(classOf[Array[String]]).newInstance(Array(""))

      // script written in kotlin script file [.kts] when compiled, assigns script result to $$result variable
      val $$resultField = clazz.getDeclaredField("$$result")
      $$resultField.setAccessible(true)

      type Script = { val getScriptDsl: ScriptDsl }
      type Result = { def invoke(services: CswServices): Script }
      // todo: see if there is other way than using structural types without adding `script-dsl` dependency on this project
      val result = $$resultField.get(script).asInstanceOf[Result]
      result.invoke(cswServices).getScriptDsl
    }

  // this loads .kt or class file
  def loadClass(scriptClass: String, cswServices: CswServices): ScriptDsl =
    withScript(scriptClass) { clazz =>
      clazz.getConstructor(classOf[CswServices]).newInstance(cswServices).asInstanceOf[ScriptDsl]
    }

  private def withScript[T](scriptClass: String)(block: Class[_] => T): T =
    try {
      val clazz = Class.forName(scriptClass)
      block(clazz)
    } catch {
      case _: ClassCastException     => throw new InvalidScriptException(scriptClass)
      case _: ClassNotFoundException => throw new ScriptNotFound(scriptClass)
    }

}
