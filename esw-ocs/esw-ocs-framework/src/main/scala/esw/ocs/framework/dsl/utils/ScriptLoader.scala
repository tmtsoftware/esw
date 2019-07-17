package esw.ocs.framework.dsl.utils

import esw.ocs.framework.dsl.{CswServices, Script}
import esw.ocs.framework.exceptions.ScriptLoadingException.{InvalidScriptException, ScriptNotFound}

private[framework] object ScriptLoader {

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
