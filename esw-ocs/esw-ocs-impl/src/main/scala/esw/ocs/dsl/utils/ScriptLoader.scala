package esw.ocs.dsl.utils

import esw.ocs.dsl.{CswServices, Script}
import esw.ocs.exceptions.ScriptLoadingException.{InvalidScriptException, ScriptNotFound}

private[ocs] object ScriptLoader {

  def load(scriptClass: String, cswServices: CswServices): Script = {
    try {
      val clazz = getClass.getClassLoader.loadClass(scriptClass)
      clazz.getConstructor(classOf[CswServices]).newInstance(cswServices).asInstanceOf[Script]
    } catch {
      case _: ClassCastException     => throw new InvalidScriptException(scriptClass)
      case _: ClassNotFoundException => throw new ScriptNotFound(scriptClass)
    }
  }
}
