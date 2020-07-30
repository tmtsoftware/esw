package esw.agent.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.prefix.models.Prefix
import esw.agent.api._
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

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
      probe.expectMessage(Failed(s"Failed to verify component registration in location service, reason: $err"))

      //ensure component is NOT registered
      verify(locationService, never).register(redisRegistration)
    }

    "reply 'Failed' and not spawn new process when it is already registered with location service | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor3")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(redisConn), any[FiniteDuration]))
        .thenReturn(redisLocationF)

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(
        Failed(s"Component ${componentId.fullName} is already registered with location service at location $redisLocation")
      )

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
      probe2.expectMessage(Failed(s"Component ${redisConn.componentId.fullName} is already running on this agent"))

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
      probe.expectMessage(Failed(s"Failed to register component ${componentId.fullName} with location service, reason: failure"))

      //ensure component is registered
      verify(locationService).register(redisRegistration)
    }
  }
}
