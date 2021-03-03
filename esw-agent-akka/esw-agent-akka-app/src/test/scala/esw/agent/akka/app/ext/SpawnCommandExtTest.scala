package esw.agent.akka.app.ext

import java.nio.file.{Path, Paths}

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.models.{ComponentId, ComponentType}
import csw.prefix.models.Prefix
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
  private val sequencerScriptsVersion: String = randomString(10)
  private val eswVersion: String              = randomString(10)
  private val containerConfig: ContainerConfig = ContainerConfig(
    "org",
    "module",
    "SampleContainerCmdApp",
    "0.0.1",
    "Standalone",
    Path.of("standalone.conf"),
    isConfigLocal = true
  )

  when(versionManager.getScriptVersion).thenReturn(Future.successful(sequencerScriptsVersion))
  when(versionManager.eswVersion).thenReturn(Future.successful(eswVersion))

  private val spawnSeqMgr            = SpawnSequenceManager(replyTo, obsModeConfPath, isConfigLocal = true, None)
  private val spawnSeqMgrWithVersion = SpawnSequenceManager(replyTo, obsModeConfPath, isConfigLocal = true, Some(version))
  private val spawnSeqMgrSimulation =
    SpawnSequenceManager(replyTo, obsModeConfPath, isConfigLocal = true, None, simulation = true)
  private val spawnContainer =
    SpawnContainer(replyTo, ComponentId(Prefix("Container.testContainer"), ComponentType.Container), containerConfig)

  "SpawnCommand.executableCommandStr" must {
    val spawnSeqCompCmd =
      s"cs launch --channel $channel ocs-app:$sequencerScriptsVersion -- seqcomp -s ${prefix.subsystem} -n $compName -a $agentPrefix"
    val spawnSeqCompWithVersionCmd =
      s"cs launch --channel $channel ocs-app:$version -- seqcomp -s ${prefix.subsystem} -n $compName -a $agentPrefix"
    val spawnSeqCompSimulationCmd =
      s"cs launch --channel $channel ocs-app:$sequencerScriptsVersion -- seqcomp -s ${prefix.subsystem} -n $compName -a $agentPrefix --simulation"
    val spawnSeqMgrCmd = s"cs launch --channel $channel sequence-manager:$eswVersion -- start -o $obsModeConf -l -a $agentPrefix"
    val spawnSeqMgrWithVersionCmd =
      s"cs launch --channel $channel sequence-manager:$version -- start -o $obsModeConf -l -a $agentPrefix"
    val spawnSeqMgrSimulationCmd =
      s"cs launch --channel $channel sequence-manager:$eswVersion -- start -o $obsModeConf -l -a $agentPrefix --simulation"
    val spawnContainerCmd =
      s"cs launch ${containerConfig.orgName}::${containerConfig.deployModule}:${containerConfig.version} -r jitpack -M ${containerConfig.appName} -- --local --standalone ${containerConfig.configFilePath}"

    "SpawnCommand.executableCommandStr" must {
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
            spawnCommand.executableCommandStr(channel, agentPrefix, versionManager).futureValue should ===(
              expectedCommandStr.split(" ").toList
            )
          }
      }
    }
  }
  override protected def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.futureValue
    super.afterAll()
  }
}
