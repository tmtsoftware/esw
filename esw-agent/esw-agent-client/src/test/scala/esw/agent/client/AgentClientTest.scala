package esw.agent.client

import java.net.URI

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.Behaviors
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Service}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.SpawnCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.api.AgentCommand.SpawnCommand.SpawnSelfRegistered.SpawnSequenceComponent
import esw.agent.api.AgentCommand.{GetAgentStatus, GetComponentStatus, KillComponent}
import esw.agent.api.ComponentStatus.{Running, Stopping}
import esw.agent.api.{AgentCommand, AgentStatus, Killed, Spawned}
import org.mockito.Mockito.when
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class AgentClientTest extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  "make" should {
    "resolve the given prefix and return a new instance of AgentClient  | ESW-237" in {
      val locationService: LocationService = MockitoSugar.mock[LocationService]
      val prefix                           = Prefix("esw.test1")
      val akkaConnection                   = AkkaConnection(ComponentId(prefix, Machine))
      val agentLocation = AkkaLocation(
        akkaConnection,
        URI.create("akka://abc")
      )
      when(locationService.resolve(akkaConnection, 5.seconds)).thenReturn(
        Future.successful(
          Some(
            agentLocation
          )
        )
      )
      AgentClient.make(prefix, locationService).futureValue
    }
    "return a failed future when location service cant resolve agent  | ESW-237" in {
      val locationService: LocationService = MockitoSugar.mock[LocationService]
      val prefix                           = Prefix("esw.test1")
      val akkaConnection                   = AkkaConnection(ComponentId(prefix, Machine))
      when(locationService.resolve(akkaConnection, 5.seconds)).thenReturn(
        Future.successful(None)
      )
      val exception = intercept[RuntimeException](AgentClient.make(prefix, locationService).futureValue)
      exception.getCause.getMessage should ===(s"could not resolve $prefix")
    }
    "return a failed future when location service call fails  | ESW-237" in {
      val locationService: LocationService = MockitoSugar.mock[LocationService]
      val prefix                           = Prefix("esw.test1")
      val akkaConnection                   = AkkaConnection(ComponentId(prefix, Machine))
      when(locationService.resolve(akkaConnection, 5.seconds)).thenReturn(
        Future.failed(new RuntimeException("boom"))
      )
      val exception = intercept[RuntimeException](AgentClient.make(prefix, locationService).futureValue)
      exception.getCause.getMessage should ===(s"boom")
    }
  }

  "spawnSequenceComponent" should {
    "send SpawnSequenceComponent message to agent and return a future with agent response" in {
      val agentRef                = spawn(stubAgent)
      implicit val sch: Scheduler = system.scheduler
      val agentClient             = new AgentClient(agentRef)
      val prefix                  = Prefix("esw.test2")
      agentClient.spawnSequenceComponent(prefix).futureValue should ===(Spawned)
    }
  }

  "spawnRedis" should {
    "send SpawnRedis message to agent and return a future with agent response" in {
      val agentRef                = spawn(stubAgent)
      implicit val sch: Scheduler = system.scheduler
      val agentClient             = new AgentClient(agentRef)
      val prefix                  = Prefix("esw.test3")
      agentClient.spawnRedis(prefix, 6379, List("--port", "6379")).futureValue should ===(Spawned)
    }
  }

  "killComponent" should {
    "send KillComponent message to agent and return a future with agent response" in {
      val agentRef                = spawn(stubAgent)
      implicit val sch: Scheduler = system.scheduler
      val agentClient             = new AgentClient(agentRef)
      val componentId             = ComponentId(Prefix("esw.test3"), SequenceComponent)
      agentClient.killComponent(componentId).futureValue should ===(Killed.gracefully)
    }
  }

  "getComponentStatus" should {
    "send GetComponentStatus message to agent and return a future with agent response" in {
      val agentRef                = spawn(stubAgent)
      implicit val sch: Scheduler = system.scheduler
      val agentClient             = new AgentClient(agentRef)
      val componentId             = ComponentId(Prefix("esw.test3"), SequenceComponent)
      agentClient.getComponentStatus(componentId).futureValue should ===(Running)
    }
  }

  "getAgentStatus" should {
    "send GetAgentStatus message to agent and return a future with agent response" in {
      val agentRef                = spawn(stubAgent)
      implicit val sch: Scheduler = system.scheduler
      val agentClient             = new AgentClient(agentRef)
      val componentId             = ComponentId(Prefix("esw.comp"), Service)
      agentClient.getAgentStatus.futureValue should ===(AgentStatus(Map(componentId -> Stopping)))
    }
  }

  private def stubAgent: Behaviors.Receive[AgentCommand] = Behaviors.receiveMessagePartial[AgentCommand] {
    case SpawnSequenceComponent(replyTo, _) =>
      replyTo ! Spawned
      Behaviors.same
    case SpawnRedis(replyTo, _, _, _) =>
      replyTo ! Spawned
      Behaviors.same
    case KillComponent(replyTo, _) =>
      replyTo ! Killed.gracefully
      Behaviors.same
    case GetComponentStatus(replyTo, _) =>
      replyTo ! Running
      Behaviors.same
    case GetAgentStatus(replyTo) =>
      replyTo ! AgentStatus(Map(ComponentId(Prefix("esw.comp"), Service) -> Stopping))
      Behaviors.same
  }
}
