package esw.agent.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Service
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.KillComponent
import esw.agent.api._
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper

import scala.concurrent.duration.DurationLong

class KillManuallyRegisteredComponentTest extends AgentSetup {

  "Kill (manually registered) Component" must {

    "reply Killed after stopping a registered component gracefully | ESW-276" in {
      val agentActorRef = spawnAgentActor(name = "test-actor1")
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()

      mockLocationServiceForRedis()
      mockSuccessfulProcess(dieAfter = 1.seconds)

      //start a component
      agentActorRef ! spawnRedis(probe1.ref)
      //wait till it is registered
      probe1.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, componentId)
      //ensure it is stopped
      probe2.expectMessage(10.seconds, Killed)

      //ensure component was unregistered
      verify(locationService).unregister(redisConn)
    }

    "reply 'Killed' when registration is being performed | ESW-276" in {
      val agentActorRef = spawnAgentActor(name = "test-actor5")
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()

      mockLocationServiceForRedis(1.second) //this will ensure actor remains in registering state
      mockSuccessfulProcess(dieAfter = 10.millis)

      //start a component
      agentActorRef ! spawnRedis(probe1.ref)
      probe1.expectNoMessage(100.millis)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, componentId)
      probe2.expectMessage(Killed)
      probe1.expectMessage(Failed("Process terminated before registration was successful"))

      verify(locationService, times(2)).unregister(redisConn)
    }

    "reply 'Killed' when process is already stopping by another message [Idempotent] | ESW-276" in {
      val agentActorRef = spawnAgentActor(agentSettings, "test-actor6")
      val spawnProbe    = TestProbe[SpawnResponse]()
      val firstKiller   = TestProbe[KillResponse]()
      val secondKiller  = TestProbe[KillResponse]()

      mockLocationServiceForRedis()
      mockSuccessfulProcess(dieAfter = 1.seconds)

      //start a component
      agentActorRef ! spawnRedis(spawnProbe.ref)
      spawnProbe.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(firstKiller.ref, componentId)
      //stop the component again
      agentActorRef ! KillComponent(secondKiller.ref, componentId)

      //ensure it is stopped gracefully
      firstKiller.expectMessage(Killed)
      secondKiller.expectMessage(Killed)

      //ensure component was unregistered
      verify(locationService, timeout(500)).unregister(redisConn)
    }

    "reply 'Failed' when given component is not running on agent | ESW-276" in {
      val agentActorRef = spawnAgentActor(name = "test-actor7")
      val probe         = TestProbe[KillResponse]()

      //try to stop the component
      agentActorRef ! KillComponent(probe.ref, ComponentId(Prefix("ESW.invalid"), Service))

      //verify that response is Failure
      probe.expectMessage(Failed("given component id is not running on this agent"))

      //ensure component was NOT unregistered
      verify(locationService, never).unregister(redisConn)
    }
  }
}
