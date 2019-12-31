package agent

import java.net.URI

import agent.AgentActor.AgentState
import agent.AgentCommand.SpawnCommand
import agent.AgentCommand.SpawnCommand.SpawnSequenceComponent
import agent.Response.{Failed, Spawned}
import agent.utils.ProcessExecutor
import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.Scheduler
import csw.location.api.exceptions.OtherLocationIsRegistered
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

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class AgentActorTest extends ScalaTestWithActorTestKit with WordSpecLike with MockitoSugar {

  private val locationService       = mock[LocationService]
  private val processExecutor       = mock[ProcessExecutor]
  implicit val scheduler: Scheduler = system.scheduler

  "SpawnSequenceComponent" must {
    "spawn a new sequence component" in {
      val agentActorRef = spawn(new AgentActor(locationService, processExecutor).behavior(AgentState.empty))
      val prefix        = Prefix("tcs.tcs_darknight")
      val seqCompConn   = AkkaConnection(ComponentId(prefix, SequenceComponent))
      val seqCompLoc    = AkkaLocation(seqCompConn, new URI("some"))
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(Future.successful(Some(seqCompLoc)))
      when(processExecutor.runCommand(any[SpawnCommand])).thenReturn(Right(1234))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Spawned)
    }

    "not spawn a component if it fails to register the component" in {
      val agentActorRef = spawn(new AgentActor(locationService, processExecutor).behavior(AgentState.empty))
      val prefix        = Prefix("tcs.tcs_darknight")
      val seqCompConn   = AkkaConnection(ComponentId(prefix, SequenceComponent))
      val probe         = TestProbe[Response]()
      val locationF     = Future.failed(OtherLocationIsRegistered("error"))

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(locationF)
      when(processExecutor.runCommand(any[SpawnCommand])).thenReturn(Right(1234))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessageType[Failed]
    }
  }
}
