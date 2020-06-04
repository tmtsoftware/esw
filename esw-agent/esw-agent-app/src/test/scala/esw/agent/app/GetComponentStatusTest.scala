package esw.agent.app

import java.net.URI

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import csw.location.api.models.ComponentType.{SequenceComponent, Service}
import csw.location.api.models.Connection.{AkkaConnection, TcpConnection}
import csw.location.api.models._
import csw.location.api.scaladsl.RegistrationResult
import esw.agent.api.AgentCommand.SpawnCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.api.AgentCommand.SpawnCommand.SpawnSelfRegistered.SpawnSequenceComponent
import esw.agent.api.AgentCommand.{GetComponentStatus, KillComponent}
import esw.agent.api.ComponentStatus.{Initializing, NotAvailable, Running, Stopping}
import esw.agent.api._
import org.mockito.ArgumentMatchers.any
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper

import scala.concurrent.duration.{DurationLong, FiniteDuration}

class GetComponentStatusTest extends AgentSetup {

  "GetComponentStatus (manually registered)" must {
    val componentId                    = ComponentId(prefix, Service)
    val getStatus                      = GetComponentStatus(_, componentId)
    val connection                     = TcpConnection(componentId)
    implicit val location: TcpLocation = TcpLocation(connection, new URI("uri"))
    implicit val registrationResult: RegistrationResult =
      RegistrationResult.from(location, con => locationService.unregister(con))
    val spawnComponent = SpawnRedis(_, prefix, 6548, List.empty)

    "reply 'NotAvailable' when given component is not present on machine | ESW-286" in {
      val agentActorRef = spawnAgentActor(name = "test-actor1")
      val probe         = TestProbe[ComponentStatus]()
      agentActorRef ! getStatus(probe.ref)
      probe.expectMessage(NotAvailable)
    }

    "reply 'Initializing' when registration is being checked for given component before spawning process | ESW-286" in {
      val agentActorRef = spawnAgentActor(name = "test-actor2")
      val spawner       = TestProbe[SpawnResponse]()
      val probe         = TestProbe[ComponentStatus]()

      mockSuccessfulProcess(2.seconds)
      mockLocationService(checkDuration = 2.seconds)

      agentActorRef ! spawnComponent(spawner.ref)
      agentActorRef ! getStatus(probe.ref)
      probe.expectMessage(Initializing)
    }

    "reply 'Initializing' when registration is being performed for given component after spawning process | ESW-286" in {
      val agentActorRef = spawnAgentActor(name = "test-actor3")
      val spawner       = TestProbe[SpawnResponse]()
      val probe         = TestProbe[ComponentStatus]()

      mockSuccessfulProcess(2.seconds)
      mockLocationService(registrationDuration = 2.seconds)

      agentActorRef ! spawnComponent(spawner.ref)
      agentActorRef ! getStatus(probe.ref)
      probe.expectMessage(Initializing)
    }

    "reply 'Running' when process is running and registered | ESW-286" in {
      val agentActorRef = spawnAgentActor(name = "test-actor4")
      val spawner       = TestProbe[SpawnResponse]()
      val probe         = TestProbe[ComponentStatus]()

      mockSuccessfulProcess(5.seconds)
      mockLocationService()

      agentActorRef ! spawnComponent(spawner.ref)
      Thread.sleep(200)
      agentActorRef ! getStatus(probe.ref)
      probe.expectMessage(Running)
    }

    "reply 'Stopping' when process is stopping | ESW-286" in {
      val agentActorRef = spawnAgentActor(name = "test-actor5")
      val spawner       = TestProbe[SpawnResponse]()
      val killer        = TestProbe[KillResponse]()
      val probe         = TestProbe[ComponentStatus]()

      mockSuccessfulProcess(5.seconds)
      mockLocationService()

      agentActorRef ! spawnComponent(spawner.ref)
      Thread.sleep(200)
      agentActorRef ! KillComponent(killer.ref, componentId)
      Thread.sleep(100)
      agentActorRef ! getStatus(probe.ref)
      probe.expectMessage(Stopping)
    }
  }

  "GetComponentStatus (self registered)" must {

    val componentId                     = ComponentId(prefix, SequenceComponent)
    val getStatus                       = GetComponentStatus(_, componentId)
    val connection                      = AkkaConnection(componentId)
    implicit val location: AkkaLocation = AkkaLocation(connection, new URI("uri"))
    implicit val registrationResult: RegistrationResult =
      RegistrationResult.from(location, con => locationService.unregister(con))
    val spawnComponent: ActorRef[SpawnResponse] => SpawnSequenceComponent = SpawnSequenceComponent(_, prefix)

    "reply 'NotAvailable' when given component is not present on machine | ESW-286" in {
      val agentActorRef = spawnAgentActor(name = "test-actor6")
      val probe         = TestProbe[ComponentStatus]()
      agentActorRef ! getStatus(probe.ref)
      probe.expectMessage(NotAvailable)
    }

    "reply 'Initializing' when registration is being checked for given component before spawning process | ESW-286" in {
      val agentActorRef = spawnAgentActor(name = "test-actor7")
      val spawner       = TestProbe[SpawnResponse]()
      val probe         = TestProbe[ComponentStatus]()

      mockSuccessfulProcess(2.seconds)
      mockLocationService(checkDuration = 2.seconds)

      agentActorRef ! spawnComponent(spawner.ref)
      agentActorRef ! getStatus(probe.ref)
      probe.expectMessage(Initializing)
    }

    "reply 'Initializing' when registration is being validated for given component after spawning process | ESW-286" in {
      val agentActorRef = spawnAgentActor(name = "test-actor8")
      val spawner       = TestProbe[SpawnResponse]()
      val probe         = TestProbe[ComponentStatus]()

      mockSuccessfulProcess(2.seconds)
      mockLocationService(validationDuration = 2.seconds)

      agentActorRef ! spawnComponent(spawner.ref)
      agentActorRef ! getStatus(probe.ref)
      probe.expectMessage(Initializing)
    }

    "reply 'Running' when process is running and registered | ESW-286" in {
      val agentActorRef = spawnAgentActor(name = "test-actor9")
      val spawner       = TestProbe[SpawnResponse]()
      val probe         = TestProbe[ComponentStatus]()

      mockSuccessfulProcess(5.seconds)
      mockLocationService()

      agentActorRef ! spawnComponent(spawner.ref)
      Thread.sleep(200)
      agentActorRef ! getStatus(probe.ref)
      probe.expectMessage(Running)
    }

    "reply 'Stopping' when process is stopping | ESW-286" in {
      val agentActorRef = spawnAgentActor(name = "test-actor10")
      val spawner       = TestProbe[SpawnResponse]()
      val killer        = TestProbe[KillResponse]()
      val probe         = TestProbe[ComponentStatus]()

      mockSuccessfulProcess(5.seconds)
      mockLocationService()

      agentActorRef ! spawnComponent(spawner.ref)
      Thread.sleep(200)
      agentActorRef ! KillComponent(killer.ref, componentId)
      Thread.sleep(100)
      agentActorRef ! getStatus(probe.ref)
      probe.expectMessage(Stopping)
    }
  }

  private def mockLocationService(
      checkDuration: FiniteDuration = 0.seconds,
      registrationDuration: FiniteDuration = 0.seconds,
      validationDuration: FiniteDuration = 0.seconds
  )(implicit location: Location, registrationResult: RegistrationResult): Unit = {
    when(locationService.resolve(any[TypedConnection[Location]], any[FiniteDuration]))
      .thenReturn(delayedFuture(None, checkDuration), delayedFuture(Some(location), validationDuration))
    when(locationService.register(any[Registration]))
      .thenReturn(delayedFuture(registrationResult, registrationDuration))
  }
}
