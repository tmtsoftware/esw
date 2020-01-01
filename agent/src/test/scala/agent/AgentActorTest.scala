package agent

import java.net.URI

import agent.AgentActor.AgentState
import agent.AgentCommand.SpawnCommand
import agent.AgentCommand.SpawnCommand.SpawnSequenceComponent
import agent.Response.{Failed, Spawned}
import agent.utils.ProcessExecutor
import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.Scheduler
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentType.SequenceComponent
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.Mockito.when
import org.scalatest.MustMatchers.convertToStringMustWrapper
import org.scalatest.WordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import scala.concurrent.duration.DurationLong
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class AgentActorTest extends ScalaTestWithActorTestKit with WordSpecLike with MockitoSugar {

  private val locationService       = mock[LocationService]
  private val processExecutor       = mock[ProcessExecutor]
  private val agentSettings         = AgentSettings("/tmp", 15.seconds)
  implicit val scheduler: Scheduler = system.scheduler

  private val prefix      = Prefix("tcs.tcs_darknight")
  private val seqCompConn = AkkaConnection(ComponentId(prefix, SequenceComponent))
  private val seqCompLoc  = Future.successful(Some(AkkaLocation(seqCompConn, new URI("some"))))

  private def spawnAgentActor() = {
    spawn(new AgentActor(locationService, processExecutor, agentSettings).behavior(AgentState.empty))
  }

  "SpawnSequenceComponent" must {
    // common mocks
    when(processExecutor.killProcess(any[Long])).thenReturn(true)

    "spawn a new sequence component" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(seqCompLoc)
      when(processExecutor.runCommand(any[SpawnCommand])).thenReturn(Right(1234))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Spawned)
    }

    "not spawn a component if it fails to register itself to location service" in {
      val agentActorRef  = spawnAgentActor()
      val probe          = TestProbe[Response]()
      val failedLocation = Future.failed(new RuntimeException("error"))

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(failedLocation)
      when(processExecutor.runCommand(any[SpawnCommand])).thenReturn(Right(1234))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessageType[Failed]
    }

    "fail if component could not be spawned" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(seqCompLoc)
      when(processExecutor.runCommand(any[SpawnCommand])).thenReturn(Left(Failed("failure")))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessageType[Failed]
    }
  }
}
