package esw.agent.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models._
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.GetAgentStatus
import esw.agent.api.AgentCommand.SpawnCommand.SpawnSelfRegistered.SpawnSequenceComponent
import esw.agent.api.ComponentStatus.Initializing
import esw.agent.api._
import org.mockito.ArgumentMatchers.any
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper

import scala.concurrent.duration.{DurationLong, FiniteDuration}

class GetAgentStatusTest extends AgentSetup {

  "GetAgentStatus" must {

    "reply with a collection of status of all components available on the agent | ESW-286" in {
      val prefix1      = Prefix("csw.component1")
      val componentId1 = ComponentId(prefix1, SequenceComponent)

      val prefix2      = Prefix("csw.component2")
      val componentId2 = ComponentId(prefix2, SequenceComponent)

      val agentActorRef = spawnAgentActor()
      val spawner       = TestProbe[SpawnResponse]()
      val probe         = TestProbe[AgentStatus]()

      when(locationService.resolve(any[TypedConnection[AkkaLocation]], any[FiniteDuration]))
        .thenReturn(delayedFuture(None, 2.seconds))

      //spawn two processes
      agentActorRef ! SpawnSequenceComponent(spawner.ref, prefix1)
      agentActorRef ! SpawnSequenceComponent(spawner.ref, prefix2)

      //get agent status
      agentActorRef ! GetAgentStatus(probe.ref)

      //ensure both components are initializing
      probe.expectMessage(AgentStatus(Map(componentId1 -> Initializing, componentId2 -> Initializing)))
    }
  }
}
