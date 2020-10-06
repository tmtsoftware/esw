package esw.script.launcher

import java.util

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.AkkaConnection
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.akka.app.ext.ProcessExt.ProcessOps
import esw.ocs.testkit.EswTestKit
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import os.{Path, up}

import scala.concurrent.duration.DurationInt

class SequencerScriptLauncherTest extends EswTestKit {

  private val className = "SampleScript"

  private val ocsAppVersion        = "0.1.0-SNAPSHOT"
  private val tmtCsChannel: String = "file://" + getClass.getResource("/apps.json").getPath

  private val sampleScriptPath = getClass.getResource(s"/$className.kts").getPath
  private val projectRootPath  = Path(sampleScriptPath) / up / up / up / up / up
  private val scriptLauncher   = (projectRootPath / "scripts" / "script-launcher" / "launchSequencer.sh").toString()

  var process: Process = _
  LoggingSystemFactory.forTestingOnly()
  "launch sequencer script should compile given sequencer script and start sequencer with it| ESW-150" in {
    val builder = new ProcessBuilder(scriptLauncher, "-f", sampleScriptPath, "-v", ocsAppVersion).inheritIO()

    // setup needed environment variables
    val processEnvironment: util.Map[String, String] = builder.environment()
    processEnvironment.put("INTERFACE_NAME", "")        // keeping it blank will auto pick the interface name
    processEnvironment.put("PUBLIC_INTERFACE_NAME", "") // keeping it blank will auto pick the interface name
    processEnvironment.put("TMT_LOG_HOME", "/tmp/csw/")
    processEnvironment.put("CS_CHANNEL", tmtCsChannel) // use local descriptor channel for testing

    process = builder.start() // start the launcher process

    // check sequencer is registered in location service
    val prefix         = Prefix(ESW, className)
    val resolveTimeout = 30.seconds // this timeout includes time taken by the process to start
    val locationF =
      locationService.resolve(AkkaConnection(ComponentId(prefix, Sequencer)), resolveTimeout)
    locationF.futureValue(Timeout(resolveTimeout)).value.prefix shouldBe prefix
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    process.toHandle.kill(10.seconds)
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
