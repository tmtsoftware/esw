package esw.ocs.dsl.utils

import esw.ocs.dsl.javadsl.JScript
import esw.ocs.dsl.{CswServices, Script, ScriptDsl}
import esw.ocs.exceptions.ScriptLoadingException.{InvalidScriptException, ScriptNotFound}

private[ocs] object ScriptLoader {

  private def load0[T <: ScriptDsl](scriptClass: String, cswServices: CswServices): T = {
    try {
      val clazz = getClass.getClassLoader.loadClass(scriptClass)
      clazz.getConstructor(classOf[CswServices]).newInstance(cswServices).asInstanceOf[T]
    } catch {
      case _: ClassCastException     => throw new InvalidScriptException(scriptClass)
      case _: ClassNotFoundException => throw new ScriptNotFound(scriptClass)
    }
  }

  def load(scriptClass: String, cswServices: CswServices): Script   = load0[Script](scriptClass, cswServices)
  def jLoad(scriptClass: String, cswServices: CswServices): JScript = load0[JScript](scriptClass, cswServices)

}
