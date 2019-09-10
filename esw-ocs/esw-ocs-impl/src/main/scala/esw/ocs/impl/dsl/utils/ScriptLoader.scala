package esw.ocs.impl.dsl.utils

import esw.ocs.impl.dsl.javadsl.JScript
import esw.ocs.impl.dsl.{CswServices, Script, ScriptDsl}
import esw.ocs.impl.exceptions.ScriptLoadingException.{InvalidScriptException, ScriptNotFound}

import scala.language.reflectiveCalls

private[ocs] object ScriptLoader {

  def load(scriptClass: String, cswServices: CswServices): Script   = load0[Script](scriptClass, cswServices)
  def jLoad(scriptClass: String, cswServices: CswServices): JScript = load0[JScript](scriptClass, cswServices)

  def loadKotlinScript(scriptClass: String, cswServices: CswServices): JScript =
    withScript(scriptClass) { clazz =>
      val script = clazz.getConstructor(classOf[Array[String]]).newInstance(Array(""))

      // script written in kotlin script file [.kts] when compiled, assigns script result to $$result variable
      val $$resultField = clazz.getDeclaredField("$$result")
      $$resultField.setAccessible(true)

      type ScriptKt = { val getJScript: JScript }
      type Result   = { def invoke(services: CswServices): ScriptKt }
      // todo: see if there is other way than using structural types without adding `script-dsl` dependency on this project
      val result = $$resultField.get(script).asInstanceOf[Result]
      result.invoke(cswServices).getJScript
    }

  private def load0[T <: ScriptDsl](scriptClass: String, cswServices: CswServices): T =
    withScript(scriptClass) { clazz =>
      clazz.getConstructor(classOf[CswServices]).newInstance(cswServices).asInstanceOf[T]
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
