package esw.agent.app

import java.net.URI

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.Scheduler
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentType.SequenceComponent
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId}
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.SpawnCommand
import esw.agent.api.AgentCommand.SpawnCommand.SpawnSequenceComponent
import esw.agent.api.Response
import esw.agent.api.Response.{Failed, Spawned}
import esw.agent.app.AgentActor.AgentState
import esw.agent.app.utils.ProcessExecutor
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.Mockito.when
import org.scalatest.MustMatchers.convertToStringMustWrapper
import org.scalatest.WordSpecLike
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}

class AgentActorTest extends ScalaTestWithActorTestKit with WordSpecLike with MockitoSugar {

  private val locationService       = mock[LocationService]
  private val processExecutor       = mock[ProcessExecutor]
  private val logger                = mock[Logger]
  private val agentSettings         = AgentSettings("/tmp", 15.seconds)
  implicit val scheduler: Scheduler = system.scheduler

  private val prefix      = Prefix("tcs.tcs_darknight")
  private val seqCompConn = AkkaConnection(ComponentId(prefix, SequenceComponent))
  private val seqCompLoc  = Future.successful(Some(AkkaLocation(seqCompConn, new URI("some"))))

  private def spawnAgentActor() = {
    spawn(new AgentActor(locationService, processExecutor, agentSettings, logger).behavior(AgentState.empty))
  }

  "SpawnSequenceComponent" must {
    // common mocks
    when(processExecutor.killProcess(any[Long])).thenReturn(true)

    "spawn a new sequence component | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLoc)
      when(processExecutor.runCommand(any[SpawnCommand])).thenReturn(Right(1234))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Spawned)
    }

    "not spawn a component if it is already registered with location servicce | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(seqCompLoc)

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessageType[Failed]
    }

    "not spawn a component if it fails to register itself to location service | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()
      val noLocation    = Future.successful(None)

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), noLocation)

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessageType[Failed]
    }

    "fail if component could not be spawned | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLoc)
      when(processExecutor.runCommand(any[SpawnCommand])).thenReturn(Left(Failed("failure")))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessageType[Failed]
    }

    "fail when location service call fails" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.failed(new RuntimeException("call failed")))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessageType[Failed]
    }
  }
}
