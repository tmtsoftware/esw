package esw.agent.akka.app.process

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix
import esw.agent.akka.app.AgentSettings
import esw.agent.service.api.models.Failed
import esw.testcommons.BaseTestSuite

class ProcessManagerTest extends BaseTestSuite {

  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "process-manager")
  private implicit val logger: Logger                                  = mock[Logger]

  private val compName   = randomString(10)
  private val compPrefix = Prefix(randomSubsystem, compName)
  private val compId     = ComponentId(compPrefix, SequenceComponent)
  private val connection = AkkaConnection(compId)
  private val uri        = new URI("some")

  private val processExecutor = mock[ProcessExecutor]
  private val locationService = mock[LocationService]
  private val agentSetting    = mock[AgentSettings]

  override protected def afterAll(): Unit = {
    actorSystem.terminate()
    actorSystem.whenTerminated.futureValue
  }

  "kill" must {
    "return failed response when location does not contain pid | ESW-367" in {
      val location = AkkaLocation(connection, uri, Metadata.empty)
      val manager  = new ProcessManager(locationService, processExecutor, agentSetting)
      manager.kill(location).futureValue should ===(Failed(s"$location metadata does not contain Pid"))
    }

    "return failed response when pid does not exist on agent machine | ESW-276, ESW-367" in {
      val pid      = "12345"
      val location = AkkaLocation(connection, uri, Metadata().withPID(pid))
      val manager = new ProcessManager(locationService, processExecutor, agentSetting) {
        override def processHandle(pid: String): Option[ProcessHandle] = None
      }
      manager.kill(location).futureValue should ===(Failed(s"Pid:$pid process does not exist"))
    }

    "return failed response when creating processHandle from pid throws an exception | ESW-367" in {
      val pid      = "12345"
      val location = AkkaLocation(connection, uri, Metadata().withPID(pid))
      val manager = new ProcessManager(locationService, processExecutor, agentSetting) {
        override def processHandle(pid: String): Option[ProcessHandle] = throw new SecurityException("Permission denied")
      }
      manager.kill(location).futureValue should ===(Failed("Permission denied"))
    }

    "return failed response when process.kill throws an exception | ESW-367" in {
      val pid      = "12345"
      val process  = mock[ProcessHandle]
      val location = AkkaLocation(connection, uri, Metadata().withPID(pid))

      val manager = new ProcessManager(locationService, processExecutor, agentSetting) {
        override def processHandle(pid: String): Option[ProcessHandle] = Some(process)
      }

      when(process.descendants()).thenThrow(new SecurityException("Permission denied"))
      manager.kill(location).futureValue should ===(
        Failed("Failed to kill component process, reason: Permission denied")
      )
    }
  }

}
