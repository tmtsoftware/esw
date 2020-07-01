package esw.script.launcher

import java.util

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.app.ext.ProcessExt.ProcessOps
import esw.commons.Timeouts
import esw.ocs.testkit.EswTestKit
import os.{Path, up}

import scala.concurrent.duration.DurationInt

class SequencerScriptLauncherTest extends EswTestKit {

  private val className        = "SampleScript"
  private val ocsAppVersion    = "e7ddebd"
  private val sampleScriptPath = getClass.getResource(s"/$className.kts").getPath
  private val projectRootPath  = Path(sampleScriptPath) / up / up / up / up / up
  private val scriptLauncher   = (projectRootPath / "scripts" / "script-launcher" / "launchSequencer.sh").toString()

  var process: Process = _

  "launch sequencer script should start sequencer with given script | ESW-150" in {
    // fetch upfront to prevent timing out
    new ProcessBuilder("cs", "fetch", s"ocs-app:$ocsAppVersion").inheritIO().start().waitFor()

    //  todo : add a step of fetch to fix the time out error
    val builder = new ProcessBuilder(scriptLauncher, "-f", sampleScriptPath, "-v", ocsAppVersion).inheritIO()

    // setup needed environment variables
    val processEnvironment: util.Map[String, String] = builder.environment()
    processEnvironment.put("INTERFACE_NAME", "")        // keeping it blank will auto pick the interface name
    processEnvironment.put("PUBLIC_INTERFACE_NAME", "") // keeping it blank will auto pick the interface name
    processEnvironment.put("TMT_LOG_HOME", "/tmp/csw/")

    process = builder.start() // start the launcher process
    Thread.sleep(3000)        // wait till process boots up

    // check sequencer is registered in location service
    val prefix = Prefix(ESW, className)
    val locationF =
      locationService.resolve(AkkaConnection(ComponentId(prefix, Sequencer)), Timeouts.DefaultTimeout)
    locationF.futureValue.value.prefix shouldBe prefix
  }

  override def afterAll(): Unit = {
    process.kill(10.seconds)
    removeJar()
    super.afterAll()
  }

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
