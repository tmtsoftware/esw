package esw.script.launcher

import java.util

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.testkit.EswTestKit
import os.{Path, proc, up}

import scala.concurrent.duration.DurationInt

class SequencerScriptLauncherTest extends EswTestKit {

  private val className        = "SampleScript"
  private val sampleScriptPath = getClass.getResource(s"/$className.kts").getPath
  private val projectRootPath  = Path(sampleScriptPath) / up / up / up / up / up
  private val scriptLauncher   = (projectRootPath / "scripts" / "script-launcher" / "launchSequencer.sh").toString()

  var process: Process = _

  "launch sequencer script should start sequencer with given script | ESW-150" in {
    val builder = new ProcessBuilder(scriptLauncher, "-f", sampleScriptPath, "-v", "e7ddebd")

    // setup needed environment variables
    val processEnvironment: util.Map[String, String] = builder.environment()
    processEnvironment.put("INTERFACE_NAME", "")        // keeping it blank will auto pick the interface name
    processEnvironment.put("PUBLIC_INTERFACE_NAME", "") // keeping it blank will auto pick the interface name
    processEnvironment.put("TMT_LOG_HOME", "/tmp/csw/")

    // start the launcher process
    process = builder.start()

    // check sequencer is registered in location service
    val prefix    = Prefix(ESW, className)
    val locationF = locationService.resolve(AkkaConnection(ComponentId(prefix, Sequencer)), 10.seconds)
    locationF.futureValue.get.prefix shouldBe prefix
  }

  override def afterAll(): Unit = {
    killSequencerProcess()
    process.destroyForcibly().waitFor() // shutdown the process
    removeJar()
    super.afterAll()
  }

  private def killSequencerProcess(): Unit =
    proc("jps", "-m")
      .call()
      .chunks
      .collect {
        case Left(s)  => s
        case Right(s) => s
      }
      .map(b => new String(b.array).trim)
      .filter(_.contains(s"-m $className"))   // get the process running the SampleScript
      .map({ x => x.split(" ").head.toLong }) // extract out the PId
      .foreach(proc("kill", "-9", _).call())  // kill the porcess

  private def removeJar(): Unit = {
    val jarName = className + ".jar"

    // this is happens when tests are run from sbt shell
    val jarInsideTestModule: Path = projectRootPath / "esw-integration-test" / jarName
    if (os.isFile(jarInsideTestModule)) os.remove(jarInsideTestModule)

    // this is happens when tests are run from intellij
    val jarOnToplevel: Path = projectRootPath / jarName
    if (os.isFile(jarOnToplevel)) os.remove(jarOnToplevel)
  }
}
