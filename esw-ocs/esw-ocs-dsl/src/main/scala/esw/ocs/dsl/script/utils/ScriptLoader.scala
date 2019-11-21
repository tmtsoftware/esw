package esw.ocs.dsl.script.utils

import esw.ocs.dsl.script.exceptions.ScriptLoadingException.{InvalidScriptException, ScriptNotFound}
import esw.ocs.dsl.script.{CswServices, JScriptDsl}

import scala.language.reflectiveCalls

private[esw] object ScriptLoader {

  // this loads .kts script
  def loadKotlinScript(scriptClass: String, cswServices: CswServices): JScriptDsl =
    withScript(scriptClass) { clazz =>
      val script = clazz.getConstructor(classOf[Array[String]]).newInstance(Array(""))

      // script written in kotlin script file [.kts] when compiled, assigns script result to $$result variable
      val $$resultField = clazz.getDeclaredField("$$result")
      $$resultField.setAccessible(true)

      type Script = { val scriptDsl: JScriptDsl }
      type Result = { def invoke(services: CswServices): Script }
      val result = $$resultField.get(script).asInstanceOf[Result]
      result.invoke(cswServices).scriptDsl
    }

  def withScript[T](scriptClass: String)(block: Class[_] => T): T =
    try {
      val clazz = Class.forName(scriptClass)
      block(clazz)
    } catch {
      case _: ClassNotFoundException => throw new ScriptNotFound(scriptClass)
      case _: NoSuchFieldException   => throw new InvalidScriptException(scriptClass)
    }
}
