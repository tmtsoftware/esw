package esw.agent.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Service
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.KillComponent
import esw.agent.api._
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper

import scala.concurrent.duration.DurationLong

//todo: fix test names
class KillManuallyRegisteredComponentTest extends AgentSetup {

  "Kill (manually registered) Component" must {

    "reply Killed after stopping a registered component gracefully | ESW-276" in {
      val agentActorRef = spawnAgentActor(name = "test-actor1")
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()

      mockLocationServiceForRedis()

      mockSuccessfulProcess(dieAfter = 2.seconds)

      //start a component
      agentActorRef ! spawnRedis(probe1.ref)
      //wait it it is registered
      probe1.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, componentId)
      //ensure it is stopped
      probe2.expectMessage(10.seconds, Killed)

      //ensure component was unregistered
      verify(locationService).unregister(redisConn)
    }

    "reply Killed after killing a running component when component is waiting registration completion | ESW-276" in {
      val agentActorRef = spawnAgentActor(name = "test-actor3")
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()

      mockLocationServiceForRedis(1.hour)

      mockSuccessfulProcess(dieAfter = 3.seconds)

      //start a component
      agentActorRef ! spawnRedis(probe1.ref)
      //it should not be registered
      probe1.expectNoMessage(2.seconds)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, componentId)
      //ensure it is stopped gracefully
      probe2.expectMessage(10.seconds, Killed)

      //ensure component was unregistered
      verify(locationService).unregister(redisConn)
    }

    "reply Killed, unregister the component and kill the component when registration is being performed | ESW-276" in {
      val agentActorRef = spawnAgentActor(name = "test-actor5")
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()

      mockLocationServiceForRedis(1.hour) //this will ensure actor remains in registering state
      mockSuccessfulProcess(2.seconds)

      //start a component
      agentActorRef ! spawnRedis(probe1.ref)
      probe1.expectNoMessage(1.seconds)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, componentId)
      //ensure it is stopped gracefully
      probe2.expectMessage(10.seconds, Killed)

      //ensure component was unregistered
      verify(locationService).unregister(redisConn)
    }

    "reply 'Failed' when process is already stopping by another message | ESW-276" in {
      val agentActorRef = spawnAgentActor(agentSettings, "test-actor6")
      val spawnProbe    = TestProbe[SpawnResponse]()
      val firstKiller   = TestProbe[KillResponse]()
      val secondKiller  = TestProbe[KillResponse]()

      mockLocationServiceForRedis()
      mockSuccessfulProcess(dieAfter = 2.seconds)

      //start a component
      agentActorRef ! spawnRedis(spawnProbe.ref)
      spawnProbe.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(firstKiller.ref, componentId)
      //stop the component again
      agentActorRef ! KillComponent(secondKiller.ref, componentId)

      //ensure it is stopped gracefully
      firstKiller.expectMessage(3.seconds, Killed)
      secondKiller.expectMessage(Failed("process is already stopping"))

      //ensure component was unregistered
      verify(locationService).unregister(redisConn)
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
