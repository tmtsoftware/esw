package esw.agent.akka.app.ext

import java.nio.file.{Path, Paths}

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.prefix.models.Prefix
import esw.agent.akka.app.AgentSettings
import esw.agent.akka.app.ext.SpawnCommandExt.SpawnCommandOps
import esw.agent.akka.client.AgentCommand.SpawnCommand.{SpawnContainer, SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.akka.client.models.ContainerConfig
import esw.agent.service.api.models.SpawnResponse
import esw.commons.utils.config.VersionManager
import esw.testcommons.BaseTestSuite
import org.scalatest.prop.Tables.Table

import scala.concurrent.{ExecutionContext, Future}

class SpawnCommandExtTest extends BaseTestSuite {
  private val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "location-service-system")
  private implicit val ec: ExecutionContext              = system.executionContext

  private val replyTo         = mock[ActorRef[SpawnResponse]]
  private val compName        = "dummy"
  private val channel         = "https://github.com/apps.json"
  private val version         = "1.0.0"
  private val obsModeConf     = "obsMode.conf"
  private val obsModeConfPath = Paths.get(obsModeConf)
  private val agentPrefix     = Prefix(randomSubsystem, randomString(10))
  private val prefix          = Prefix(agentPrefix.subsystem, compName)

  private val spawnSeqComp            = SpawnSequenceComponent(replyTo, agentPrefix, compName, None)
  private val spawnSeqCompWithVersion = SpawnSequenceComponent(replyTo, agentPrefix, compName, Some(version))
  private val spawnSeqCompSimulation  = SpawnSequenceComponent(replyTo, agentPrefix, compName, None, simulation = true)

  private val versionManager: VersionManager  = mock[VersionManager]
  private val versionConfPath: Path           = Path.of(randomString(30))
  private val sequencerScriptsVersion: String = randomString(10)
  private val containerConfig: ContainerConfig = ContainerConfig(
    "org",
    "module",
    "SampleContainerCmdApp",
    "0.0.1",
    "Standalone",
    Path.of("container.conf"),
    isConfigLocal = true
  )

  when(versionManager.getScriptVersion(versionConfPath)).thenReturn(Future.successful(sequencerScriptsVersion))

  val spawnSeqMgr            = SpawnSequenceManager(replyTo, obsModeConfPath, isConfigLocal = true, None)
  val spawnSeqMgrWithVersion = SpawnSequenceManager(replyTo, obsModeConfPath, isConfigLocal = true, Some(version))
  val spawnSeqMgrSimulation =
    SpawnSequenceManager(replyTo, obsModeConfPath, isConfigLocal = true, None, simulation = true)
  val spawnContainer = SpawnContainer(replyTo, containerConfig)

  "SpawnCommand.executableCommandStr" must {
    val spawnSeqCompCmd =
      s"cs launch --channel $channel ocs-app:$sequencerScriptsVersion -- seqcomp -s ${prefix.subsystem} -n $compName -a $agentPrefix"
    val spawnSeqCompWithVersionCmd =
      s"cs launch --channel $channel ocs-app:$version -- seqcomp -s ${prefix.subsystem} -n $compName -a $agentPrefix"
    val spawnSeqCompSimulationCmd =
      s"cs launch --channel $channel ocs-app:$sequencerScriptsVersion -- seqcomp -s ${prefix.subsystem} -n $compName -a $agentPrefix --simulation"
    val spawnSeqMgrCmd = s"cs launch --channel $channel sequence-manager -- start -o $obsModeConf -l -a $agentPrefix"
    val spawnSeqMgrWithVersionCmd =
      s"cs launch --channel $channel sequence-manager:$version -- start -o $obsModeConf -l -a $agentPrefix"
    val spawnSeqMgrSimulationCmd =
      s"cs launch --channel $channel sequence-manager -- start -o $obsModeConf -l -a $agentPrefix --simulation"
    val spawnContainerCmd =
      s"cs launch ${containerConfig.orgName}::${containerConfig.deployModule}:${containerConfig.version} -r jitpack -M ${containerConfig.appName} -- --local ${containerConfig.configFilePath}"
    val agentSettings = AgentSettings(agentPrefix, channel, versionConfPath, gcMetricsEnabled = false)

    Table(
      ("TestName", "SpawnCommand", "ExpectedCommandStr"),
      ("SpawnSequenceComponent", spawnSeqComp, spawnSeqCompCmd),
      ("SpawnSequenceComponent(version)", spawnSeqCompWithVersion, spawnSeqCompWithVersionCmd),
      ("SpawnSequenceComponentSimulation", spawnSeqCompSimulation, spawnSeqCompSimulationCmd),
      ("SpawnSequenceManager", spawnSeqMgr, spawnSeqMgrCmd),
      ("SpawnSequenceManager(version)", spawnSeqMgrWithVersion, spawnSeqMgrWithVersionCmd),
      ("SpawnSequenceManagerSimulation", spawnSeqMgrSimulation, spawnSeqMgrSimulationCmd),
      ("SpawnContainer", spawnContainer, spawnContainerCmd)
    ).foreach {
      case (name, spawnCommand, expectedCommandStr) =>
        name in {
          spawnCommand.executableCommandStr(agentSettings, versionManager).futureValue should ===(
            expectedCommandStr.split(" ").toList
          )
        }
    }
  }

  "SpawnCommand.executableCommandStr" must {
    val gcLogDirString                 = System.getProperty("user.home") + "/gc-metrics"
    def gcLogFileName(appName: String) = gcLogDirString + "/" + appName + "_gc.txt"

    val agentSettingsWithGCMetricsEnabled = AgentSettings(agentPrefix, channel, versionConfPath, gcMetricsEnabled = true)
    val spawnSeqCompCmdWithGCMetrics =
      s"cs launch --java-opt -Xlog:gc:file=${gcLogFileName("ocs-app")}::filecount=1 --channel $channel ocs-app:$sequencerScriptsVersion -- seqcomp -s ${prefix.subsystem} -n $compName -a $agentPrefix"
    val spawnSeqCompWithVersionCmdWithGCMetrics =
      s"cs launch --java-opt -Xlog:gc:file=${gcLogFileName(
        "ocs-app"
      )}::filecount=1 --channel $channel ocs-app:$version -- seqcomp -s ${prefix.subsystem} -n $compName -a $agentPrefix"
    val spawnSeqCompSimulationCmdWithGCMetrics =
      s"cs launch --java-opt -Xlog:gc:file=${gcLogFileName("ocs-app")}::filecount=1 --channel $channel ocs-app:$sequencerScriptsVersion -- seqcomp -s ${prefix.subsystem} -n $compName -a $agentPrefix --simulation"
    val spawnSeqMgrCmdWithGCMetrics =
      s"cs launch --java-opt -Xlog:gc:file=${gcLogFileName("sequence-manager")}::filecount=1 --channel $channel sequence-manager -- start -o $obsModeConf -l -a $agentPrefix"
    val spawnSeqMgrWithVersionCmdWithGCMetrics =
      s"cs launch --java-opt -Xlog:gc:file=${gcLogFileName("sequence-manager")}::filecount=1 --channel $channel sequence-manager:$version -- start -o $obsModeConf -l -a $agentPrefix"
    val spawnSeqMgrSimulationCmdWithGCMetrics =
      s"cs launch --java-opt -Xlog:gc:file=${gcLogFileName("sequence-manager")}::filecount=1 --channel $channel sequence-manager -- start -o $obsModeConf -l -a $agentPrefix --simulation"
    val spawnContainerCmdWithGCMetrics =
      s"cs launch --java-opt -Xlog:gc:file=${gcLogFileName(
        s"${containerConfig.orgName}::${containerConfig.deployModule}"
      )}::filecount=1 ${containerConfig.orgName}::${containerConfig.deployModule}:${containerConfig.version} -r jitpack -M ${containerConfig.appName} -- --local ${containerConfig.configFilePath}"

    Table(
      ("Command", "SpawnCommand", "ExpectedCommandStr"),
      ("SpawnSequenceComponent", spawnSeqComp, spawnSeqCompCmdWithGCMetrics),
      ("SpawnSequenceComponent(version)", spawnSeqCompWithVersion, spawnSeqCompWithVersionCmdWithGCMetrics),
      ("SpawnSequenceComponentSimulation", spawnSeqCompSimulation, spawnSeqCompSimulationCmdWithGCMetrics),
      ("SpawnSequenceManager", spawnSeqMgr, spawnSeqMgrCmdWithGCMetrics),
      ("SpawnSequenceManager(version)", spawnSeqMgrWithVersion, spawnSeqMgrWithVersionCmdWithGCMetrics),
      ("SpawnSequenceManagerSimulation", spawnSeqMgrSimulation, spawnSeqMgrSimulationCmdWithGCMetrics),
      ("SpawnContainer", spawnContainer, spawnContainerCmdWithGCMetrics)
    ).foreach {
      case (command, spawnCommand, expectedCommandStr) =>
        s"$command with GC options enabled" in {
          spawnCommand.executableCommandStr(agentSettingsWithGCMetricsEnabled, versionManager).futureValue should ===(
            expectedCommandStr.split(" ").toList
          )
        }
    }
  }

  override protected def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.futureValue
    super.afterAll()
  }
}
