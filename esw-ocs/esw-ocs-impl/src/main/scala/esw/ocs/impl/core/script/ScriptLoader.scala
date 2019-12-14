package esw.ocs.impl.core.script

import esw.ocs.impl.core.script.ScriptLoadingException.{InvalidScriptException, ScriptNotFound}

import scala.language.reflectiveCalls

private[esw] object ScriptLoader {

  // this loads .kts script
  def loadKotlinScript(scriptClass: String, scriptContext: ScriptContext): ScriptApi =
    withScript(scriptClass) { clazz =>
      val script = clazz.getConstructor(classOf[Array[String]]).newInstance(Array(""))

      // script written in kotlin script file [.kts] when compiled, assigns script result to $$result variable
      val $$resultField = clazz.getDeclaredField("$$result")
      $$resultField.setAccessible(true)

      type Result = { def invoke(context: ScriptContext): ScriptApi }
      val result = $$resultField.get(script).asInstanceOf[Result]
      result.invoke(scriptContext)
    }

  def withScript[T](scriptClass: String)(block: Class[_] => T): T =
    try {
      val clazz = Class.forName(scriptClass)
      block(clazz)
    }
    catch {
      case _: ClassNotFoundException => throw new ScriptNotFound(scriptClass)
      case _: NoSuchFieldException   => throw new InvalidScriptException(scriptClass)
    }
}
