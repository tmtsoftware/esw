package esw.ocs.impl.script

import esw.ocs.impl.pyscript.{PyScriptApi, PyScriptApiWrapper}

import scala.language.reflectiveCalls
import esw.ocs.impl.script.ScriptLoadingException.{InvalidScriptException, ScriptNotFound}
import org.graalvm.polyglot.{Context, Source}

import java.io.{File, FileNotFoundException}
import scala.concurrent.ExecutionContext

private[esw] object ScriptLoader {

  // We need to know where the sequencer-scripts repo is in order to load the scripts at runtime.
  // For now, require that the environment variable is set.
  private val maybeSequencerScriptsDir = sys.env.get("SEQUENCER_SCRIPTS_HOME")
  if (maybeSequencerScriptsDir.isEmpty)
    throw new RuntimeException(
      "Please set the SEQUENCER_SCRIPTS_HOME environment variable to the root of the checked out sequencer-scripts repo"
    )
  private val scriptDir = s"${maybeSequencerScriptsDir.get}/pyScripts"

  private def getScriptFile(scriptClass: String): File = {
    val scriptFile =
      if (scriptClass.startsWith("esw.ocs.scripts.examples.testData"))
        new File(
          if (new File("examples").isDirectory) "examples" else "../examples",
          scriptClass
            .replace("esw.ocs.scripts.examples.testData", "testData")
            .replace(".", "/") + ".py"
        )
      else
        new File(scriptDir, scriptClass.replace(".", "/") + ".py")
    if (!scriptFile.exists()) {
      println(s"XXX File not found: $scriptFile")
      throw new FileNotFoundException(s"File not found: $scriptFile")
    }
    scriptFile
  }

  // this loads the python script for the class name (XXX TODO: Change API to use file)
  def loadPythonScript(scriptClass: String, scriptContext: ScriptContext)(implicit ec: ExecutionContext): ScriptApi = {
    val scriptFile = getScriptFile(scriptClass)
    val context = Context
      .newBuilder("python")
      .allowAllAccess(true)
      .option("python.ForceImportSite", "true")
      .option("python.Executable", s"$scriptDir/venv/bin/graalpython")
      .build()
    val source = Source.newBuilder("python", scriptFile).build()
    val x      = context.eval(source)
    println(s"XXX x = $x")
    val clazz    = context.getPolyglotBindings.getMember("script")
    val instance = clazz.newInstance()
    val res      = instance.as(classOf[PyScriptApi])
    new PyScriptApiWrapper(res)
  }
}
