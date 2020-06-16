package esw.agent.client

import java.net.URI

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Service}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.api.AgentCommand.{GetAgentStatus, GetComponentStatus, KillComponent, SpawnCommand}
import esw.agent.api.ComponentStatus.{Running, Stopping}
import esw.agent.api.{AgentCommand, AgentStatus, Killed, Spawned}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future

class AgentClientTest extends AnyWordSpecLike with Matchers with BeforeAndAfterAll with MockitoSugar {

  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "location-service-system")

  "make" should {
    "resolve the given prefix and return a new instance of AgentClient  | ESW-237" in {
      val locationService: LocationService = mock[LocationService]
      val prefix                           = Prefix("esw.test1")
      val akkaConnection                   = AkkaConnection(ComponentId(prefix, Machine))
      val agentLocation                    = AkkaLocation(akkaConnection, URI.create("akka://abc"))
      when(locationService.find(akkaConnection)).thenReturn(Future.successful(Some(agentLocation)))
      AgentClient.make(prefix, locationService).futureValue
    }

    "return a failed future when location service cant resolve agent  | ESW-237" in {
      val locationService: LocationService = mock[LocationService]
      val prefix                           = Prefix("esw.test1")
      val akkaConnection                   = AkkaConnection(ComponentId(prefix, Machine))
      when(locationService.find(akkaConnection)).thenReturn(
        Future.successful(None)
      )
      val exception = intercept[RuntimeException](AgentClient.make(prefix, locationService).futureValue)
      exception.getCause.getMessage should ===(s"could not resolve agent with prefix: $prefix")
    }

    "return a failed future when location service call fails  | ESW-237" in {
      val locationService: LocationService = mock[LocationService]
      val prefix                           = Prefix("esw.test1")
      val akkaConnection                   = AkkaConnection(ComponentId(prefix, Machine))
      when(locationService.find(akkaConnection)).thenReturn(
        Future.failed(new RuntimeException("boom"))
      )
      val exception = intercept[RuntimeException](AgentClient.make(prefix, locationService).futureValue)
      exception.getCause.getMessage should ===(s"boom")
    }
  }

  "spawnSequenceComponent" should {
    "send SpawnSequenceComponent message to agent and return a future with agent response" in {
      val agentRef                = system.systemActorOf(stubAgent, "test-agent1")
      val agentLocation           = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "test_agent_1"), Machine)), agentRef.toURI)
      implicit val sch: Scheduler = system.scheduler
      val agentClient             = new AgentClient(agentLocation)
      val prefix                  = Prefix("esw.test2")
      agentClient.spawnSequenceComponent(prefix).futureValue should ===(Spawned)
    }
  }

  "spawnRedis" should {
    "send SpawnRedis message to agent and return a future with agent response" in {
      val agentRef                = system.systemActorOf(stubAgent, "test-agent2")
      val agentLocation           = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "test_agent_2"), Machine)), agentRef.toURI)
      implicit val sch: Scheduler = system.scheduler
      val agentClient             = new AgentClient(agentLocation)
      val prefix                  = Prefix("esw.test3")
      agentClient.spawnRedis(prefix, 6379, List("--port", "6379")).futureValue should ===(Spawned)
    }
  }

  "killComponent" should {
    "send KillComponent message to agent and return a future with agent response" in {
      val agentRef                = system.systemActorOf(stubAgent, "test-agent3")
      val agentLocation           = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "test_agent_3"), Machine)), agentRef.toURI)
      implicit val sch: Scheduler = system.scheduler
      val agentClient             = new AgentClient(agentLocation)
      val componentId             = ComponentId(Prefix("esw.test3"), SequenceComponent)
      agentClient.killComponent(componentId).futureValue should ===(Killed)
    }
  }

  "getComponentStatus" should {
    "send GetComponentStatus message to agent and return a future with agent response" in {
      val agentRef                = system.systemActorOf(stubAgent, "test-agent4")
      val agentLocation           = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "test_agent_4"), Machine)), agentRef.toURI)
      implicit val sch: Scheduler = system.scheduler
      val agentClient             = new AgentClient(agentLocation)
      val componentId             = ComponentId(Prefix("esw.test3"), SequenceComponent)
      agentClient.getComponentStatus(componentId).futureValue should ===(Running)
    }
  }

  "getAgentStatus" should {
    "send GetAgentStatus message to agent and return a future with agent response" in {
      val agentRef                = system.systemActorOf(stubAgent, "test-agent5")
      val agentLocation           = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "test_agent_5"), Machine)), agentRef.toURI)
      implicit val sch: Scheduler = system.scheduler
      val agentClient             = new AgentClient(agentLocation)
      val componentId             = ComponentId(Prefix("esw.comp"), Service)
      agentClient.getAgentStatus.futureValue should ===(AgentStatus(Map(componentId -> Stopping)))
    }
  }

  private def stubAgent: Behaviors.Receive[AgentCommand] =
    Behaviors.receiveMessagePartial[AgentCommand] { msg =>
      msg match {
        case cmd: SpawnCommand              => cmd.replyTo ! Spawned
        case KillComponent(replyTo, _)      => replyTo ! Killed
        case GetComponentStatus(replyTo, _) => replyTo ! Running
        case GetAgentStatus(replyTo)        => replyTo ! AgentStatus(Map(ComponentId(Prefix("esw.comp"), Service) -> Stopping))
      }
      Behaviors.same
    }
}
