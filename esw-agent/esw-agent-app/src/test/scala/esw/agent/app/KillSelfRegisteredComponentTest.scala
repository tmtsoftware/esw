package esw.agent.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.SequenceComponent
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.KillComponent
import esw.agent.api.AgentCommand.SpawnCommand.SpawnSelfRegistered.SpawnSequenceComponent
import esw.agent.api._
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper

import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}

//todo: fix test names
class KillSelfRegisteredComponentTest extends AgentSetup {

  "Kill (self registered) Component" must {

    "reply Killed after stopping a registered component gracefully | ESW-276" in {
      val agentActorRef = spawnAgentActor(name = "test-actor1")
      val spawner       = TestProbe[SpawnResponse]()
      val killer        = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLocationF)

      mockSuccessfulProcess(dieAfter = 2.seconds)

      //start a component
      agentActorRef ! SpawnSequenceComponent(spawner.ref, seqCompPrefix)
      //wait it it is registered
      spawner.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(killer.ref, seqCompComponentId)
      //ensure it is stopped
      killer.expectMessage(10.seconds, Killed)
    }

    "reply Killed after killing a running component when component is waiting registration confirmation | ESW-276" in {
      val agentActorRef = spawnAgentActor(name = "test-actor3")
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()
      //this will actor remains in waiting state
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), delayedFuture(Some(seqCompLocation), 1.hour))

      mockSuccessfulProcess(dieAfter = 3.seconds)

      //start a component
      agentActorRef ! SpawnSequenceComponent(probe1.ref, seqCompPrefix)
      //it should not be registered
      probe1.expectNoMessage(2.seconds)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, seqCompComponentId)
      //ensure it is stopped gracefully
      probe2.expectMessage(10.seconds, Killed)
    }

    "reply Killed and cancel spawning of an already scheduled component when registration is being checked | ESW-276" in {
      val agentActorRef = spawnAgentActor(name = "test-actor5")
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(delayedFuture(None, 1.hour)) //this will actor remains in checking state

      //start a component
      agentActorRef ! SpawnSequenceComponent(probe1.ref, seqCompPrefix)
      //it should not be registered
      probe1.expectNoMessage(1.seconds)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, seqCompComponentId)
      //ensure it is stopped gracefully
      probe2.expectMessage(10.seconds, Killed)
    }

    "reply Killed after process termination, when process is already stopping by another message | ESW-276" in {
      val agentActorRef = spawnAgentActor(agentSettings, "test-actor6")
      val spawnProbe    = TestProbe[SpawnResponse]()
      val firstKiller   = TestProbe[KillResponse]()
      val secondKiller  = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLocationF)

      mockSuccessfulProcess(dieAfter = 2.seconds)

      //start a component
      agentActorRef ! SpawnSequenceComponent(spawnProbe.ref, seqCompPrefix)
      spawnProbe.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(firstKiller.ref, seqCompComponentId)
      //stop the component again
      agentActorRef ! KillComponent(secondKiller.ref, seqCompComponentId)

      //ensure it is stopped gracefully
      firstKiller.expectMessage(6.seconds, Killed)
      secondKiller.expectMessage(Failed("process is already stopping"))
    }

    "reply 'Failed' when given component is not running on agent | ESW-276" in {
      val agentActorRef = spawnAgentActor(name = "test-actor7")
      val probe         = TestProbe[KillResponse]()

      //try to stop the component
      agentActorRef ! KillComponent(probe.ref, ComponentId(Prefix("ESW.invalid"), SequenceComponent))

      //verify that response is Failure
      probe.expectMessage(Failed("given component id is not running on this agent"))
    }
  }
}
