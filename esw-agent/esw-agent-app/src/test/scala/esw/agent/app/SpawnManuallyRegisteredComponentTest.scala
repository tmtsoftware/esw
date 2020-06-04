package esw.agent.app

import java.net.URI

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.{ComponentId, TcpLocation, TcpRegistration}
import csw.location.api.scaladsl.RegistrationResult
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.KillComponent
import esw.agent.api.AgentCommand.SpawnCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.api._
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper

import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}

class SpawnManuallyRegisteredComponentTest extends AgentSetup {

  "SpawnManuallyRegistered (component)" must {

    "reply 'Spawned' and spawn component process | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor1")
      val probe         = TestProbe[SpawnResponse]()

      mockLocationServiceForRedis()
      mockSuccessfulProcess()

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(Spawned)

      //ensure component is registered
      verify(locationService).register(redisRegistration)
    }

    "reply 'Failed' and not spawn new process when `resolve` call to location service fails" in {
      val agentActorRef = spawnAgentActor(name = "test-actor2")
      val probe         = TestProbe[SpawnResponse]()
      val err           = "Failed to resolve component"
      when(locationService.resolve(argEq(redisConn), any[FiniteDuration])).thenReturn(Future.failed(new RuntimeException(err)))

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(Failed(err))

      //ensure component is NOT registered
      verify(locationService, never).register(redisRegistration)
    }

    "reply 'Failed' and not spawn new process when it is already registered with location service | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor3")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(redisConn), any[FiniteDuration]))
        .thenReturn(redisLocationF)

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(Failed("can not spawn component when it is already registered in location service"))

      //ensure component is NOT registered
      verify(locationService, never).register(redisRegistration)
    }

    "reply 'Failed' and not spawn new process when it is already spawned on the agent | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor4")
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[SpawnResponse]()

      mockLocationServiceForRedis()
      mockSuccessfulProcess()

      agentActorRef ! spawnRedis(probe1.ref)
      agentActorRef ! spawnRedis(probe2.ref)

      probe1.expectMessage(Spawned)
      probe2.expectMessage(Failed("given component is already in process"))

      //ensure redis is registered once
      verify(locationService).register(redisRegistration)
    }

    "reply 'Failed' when process fails to spawn | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor5")
      val probe         = TestProbe[SpawnResponse]()

      mockLocationServiceForRedis()

      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Left("failure"))

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(Failed("failure"))

      //ensure register is NOT registered
      verify(locationService, never).register(redisRegistration)
    }

    "reply 'Failed' and kill process, when the process is spawned but failed to register | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor6")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(redisConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None))

      when(locationService.register(redisRegistration))
        .thenReturn(Future.failed(new RuntimeException("failure")))

      mockSuccessfulProcess()

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(Failed("registration encountered an issue or timed out"))

      //ensure component is registered
      verify(locationService).register(redisRegistration)
    }

    "Unregister when process is spawned but exits before registration and registration is later succeeded | ESW-237" in {
      val agentActorRef = spawnAgentActor(agentSettings.copy(durationToWaitForComponentRegistration = 4.seconds), "test-actor7")
      // fixme: remove duplication
      val probe             = TestProbe[SpawnResponse]()
      val prefix            = Prefix("csw.redis")
      val redisConn         = TcpConnection(ComponentId(prefix, Service))
      val redisRegistration = TcpRegistration(redisConn, 100)
      val redisLocation     = TcpLocation(redisConn, new URI("some"))
      val spawnRedis        = SpawnRedis(_, prefix, 100, List.empty)

      when(locationService.resolve(argEq(redisConn), any[FiniteDuration])).thenReturn(Future.successful(None))
      when(locationService.register(redisRegistration)).thenReturn(
        delayedFuture(RegistrationResult.from(redisLocation, con => locationService.unregister(con)), 2.seconds)
      )

      mockSuccessfulProcess(dieAfter = 500.millis)

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(3.seconds, Failed("process died before registration"))

      //ensure component is registered
      verify(locationService).register(redisRegistration)

      //ensure component is unregistered later
      verify(locationService, timeout(1000)).unregister(redisConn)
    }

    "reply 'Failed' when spawning is aborted by another message | ESW-237, ESW-276" in {
      val agentActorRef = spawnAgentActor(agentSettings.copy(durationToWaitForComponentRegistration = 7.seconds), "test-actor8")
      val spawner       = TestProbe[SpawnResponse]()
      val killer        = TestProbe[KillResponse]()

      mockLocationServiceForRedis(registrationDuration = 5.seconds)
      mockSuccessfulProcess(dieAfter = 2.seconds)

      agentActorRef ! spawnRedis(spawner.ref)
      Thread.sleep(500)
      agentActorRef ! KillComponent(killer.ref, componentId)

      spawner.expectMessage(Failed("Aborted"))
      killer.expectMessage(Killed)
    }
  }
}
