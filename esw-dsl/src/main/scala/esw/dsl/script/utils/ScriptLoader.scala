package esw.dsl.script.utils

import esw.dsl.script.exceptions.ScriptLoadingException.{InvalidScriptException, ScriptNotFound}
import esw.dsl.script.{CswServices, Script}

private[esw] object ScriptLoader {

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
