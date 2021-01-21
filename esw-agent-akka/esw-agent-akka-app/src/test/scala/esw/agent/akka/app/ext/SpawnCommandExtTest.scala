package esw.agent.akka.app.ext

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.prefix.models.Prefix
import esw.agent.akka.app.ext.SpawnCommandExt.SpawnCommandOps
import esw.agent.akka.client.AgentCommand.SpawnCommand.{SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.service.api.models.SpawnResponse
import esw.commons.utils.config.VersionManager
import esw.testcommons.BaseTestSuite
import org.scalatest.prop.Tables.Table

import java.nio.file.{Path, Paths}
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

  when(versionManager.getScriptVersion(versionConfPath)).thenReturn(Future.successful(sequencerScriptsVersion))

  private val spawnSeqCompCmd =
    s"cs launch --channel $channel ocs-app:$sequencerScriptsVersion -- seqcomp -s ${prefix.subsystem} -n $compName -a $agentPrefix"
  private val spawnSeqCompWithVersionCmd =
    s"cs launch --channel $channel ocs-app:$version -- seqcomp -s ${prefix.subsystem} -n $compName -a $agentPrefix"
  private val spawnSeqCompSimulationCmd =
    s"cs launch --channel $channel ocs-app:$sequencerScriptsVersion -- seqcomp -s ${prefix.subsystem} -n $compName -a $agentPrefix --simulation"

  private val spawnSeqMgr            = SpawnSequenceManager(replyTo, obsModeConfPath, isConfigLocal = true, None)
  private val spawnSeqMgrWithVersion = SpawnSequenceManager(replyTo, obsModeConfPath, isConfigLocal = true, Some(version))
  private val spawnSeqMgrCmd         = s"cs launch --channel $channel sequence-manager -- start -o $obsModeConf -l -a $agentPrefix"
  private val spawnSeqMgrWithVersionCmd =
    s"cs launch --channel $channel sequence-manager:$version -- start -o $obsModeConf -l -a $agentPrefix"
  private val spawnSeqMgrSimulation =
    SpawnSequenceManager(replyTo, obsModeConfPath, isConfigLocal = true, None, simulation = true)
  private val spawnSeqMgrSimulationCmd =
    s"cs launch --channel $channel sequence-manager -- start -o $obsModeConf -l -a $agentPrefix --simulation"

  "SpawnCommand.executableCommandStr" must {
    Table(
      ("TestName", "SpawnCommand", "ExpectedCommandStr"),
      ("SpawnSequenceComponent", spawnSeqComp, spawnSeqCompCmd),
      ("SpawnSequenceComponent(version)", spawnSeqCompWithVersion, spawnSeqCompWithVersionCmd),
      ("SpawnSequenceComponentSimulation", spawnSeqCompSimulation, spawnSeqCompSimulationCmd),
      ("SpawnSequenceManager", spawnSeqMgr, spawnSeqMgrCmd),
      ("SpawnSequenceManager(version)", spawnSeqMgrWithVersion, spawnSeqMgrWithVersionCmd),
      ("SpawnSequenceManagerSimulation", spawnSeqMgrSimulation, spawnSeqMgrSimulationCmd)
    ).foreach {
      case (name, spawnCommand, expectedCommandStr) =>
        name in {
          spawnCommand.executableCommandStr(channel, agentPrefix, versionManager, versionConfPath).futureValue should ===(
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
