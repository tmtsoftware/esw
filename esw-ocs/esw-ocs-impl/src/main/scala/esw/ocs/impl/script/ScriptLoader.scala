package esw.ocs.impl.script

import esw.ocs.impl.script.ScriptLoadingException.{InvalidScriptException, ScriptNotFound}

import java.lang.reflect.InvocationTargetException
import scala.reflect.Selectable.reflectiveSelectable

object ScriptLoader {

  // this loads .kts script
  def loadKotlinScript(scriptClass: String, scriptContext: ScriptContext): ScriptApi =
    withScript(scriptClass) { clazz =>
      val script = clazz.getConstructor(classOf[Array[String]]).newInstance(Array(""))

      // script written in kotlin script file [.kts] when compiled, assigns script result to $$result variable
      val $$resultField = clazz.getDeclaredField("$$result")
      $$resultField.setAccessible(true)

      // See DslSupport.kt: fun invoke()
      type Result = { def invoke(context: ScriptContext): ScriptApi }
      val result = $$resultField.get(script).asInstanceOf[Result]
      try {
        result.invoke(scriptContext)
      }
      catch {
        case ex: InvocationTargetException => throw ex.getCause
      }
    }

  def withScript[T](scriptClass: String)(block: Class[?] => T): T =
    try {
      val clazz = Class.forName(scriptClass)
      block(clazz)
    }
    catch {
      case _: ClassNotFoundException => throw new ScriptNotFound(scriptClass)
      case _: NoSuchFieldException   => throw new InvalidScriptException(scriptClass)
    }
}
