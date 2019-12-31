package agent.utils.helpers

import java.io.File

import scala.reflect.ClassTag

object ScalaClassRunner {
  def run[T: ClassTag](): ProcessBuilder = {
    val javaHome           = System.getProperty("java.home")
    val javaBin            = javaHome + File.separator + "bin" + File.separator + "java"
    val classpath          = System.getProperty("java.class.path")
    val className          = implicitly[ClassTag[T]].runtimeClass.getName
    val effectiveClassName = if (className.endsWith("$")) className.substring(0, className.length - 1) else className
    val command            = List(javaBin, "-cp", classpath, effectiveClassName)
    new ProcessBuilder(command: _*)
  }
}
