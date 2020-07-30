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

class KillSelfRegisteredComponentTest extends AgentSetup {

  "Kill (self registered) Component" must {

    "reply Killed after stopping a registered component gracefully | ESW-276" in {
      val agentActorRef = spawnAgentActor(name = "test-actor1")
      val spawner       = TestProbe[SpawnResponse]()
      val killer        = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(Future.successful(None), seqCompLocationF)

      mockSuccessfulProcess(dieAfter = 1.seconds)

      //start a component
      agentActorRef ! SpawnSequenceComponent(spawner.ref, seqCompPrefix)
      //wait it it is registered
      spawner.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(killer.ref, seqCompComponentId)
      //ensure it is stopped
      killer.expectMessage(Killed)
    }

    "reply 'Killed' after process termination, when process is already stopping by another message | ESW-276" in {
      val agentActorRef = spawnAgentActor(agentSettings, "test-actor6")
      val spawnProbe    = TestProbe[SpawnResponse]()
      val firstKiller   = TestProbe[KillResponse]()
      val secondKiller  = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLocationF)

      mockSuccessfulProcess(dieAfter = 1.seconds)

      //start a component
      agentActorRef ! SpawnSequenceComponent(spawnProbe.ref, seqCompPrefix)
      spawnProbe.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(firstKiller.ref, seqCompComponentId)
      //stop the component again
      agentActorRef ! KillComponent(secondKiller.ref, seqCompComponentId)

      //ensure it is stopped gracefully
      firstKiller.expectMessage(Killed)
      secondKiller.expectMessage(Killed)
    }

    "reply 'Failed' when given component is not running on agent | ESW-276" in {
      val agentActorRef = spawnAgentActor(name = "test-actor7")
      val probe         = TestProbe[KillResponse]()

      //try to stop the component
      val id = ComponentId(Prefix("ESW.invalid"), SequenceComponent)
      agentActorRef ! KillComponent(probe.ref, id)

      //verify that response is Failure
      probe.expectMessage(Failed(s"Component ${id.fullName} is not running on this agent"))
    }
  }
}
