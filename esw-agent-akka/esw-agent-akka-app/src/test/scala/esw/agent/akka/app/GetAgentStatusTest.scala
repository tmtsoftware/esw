package esw.agent.akka.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models._
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentCommand.GetAgentStatus
import esw.agent.akka.client.AgentCommand.SpawnCommand.SpawnSelfRegistered.SpawnSequenceComponent
import esw.agent.service.api.ComponentStatus.Initializing
import esw.agent.service.api.{AgentStatus, SpawnResponse}
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

      val agentPrefixStr = "ESW.dummy-agent"
      val agentActorRef  = spawnAgentActor()
      val spawner        = TestProbe[SpawnResponse]()
      val probe          = TestProbe[AgentStatus]()

      when(locationService.resolve(any[TypedConnection[AkkaLocation]], any[FiniteDuration]))
        .thenReturn(delayedFuture(None, 2.seconds))

      //spawn two processes
      agentActorRef ! SpawnSequenceComponent(spawner.ref, agentPrefixStr, prefix1, None)
      agentActorRef ! SpawnSequenceComponent(spawner.ref, agentPrefixStr, prefix2, None)

      //get agent status
      agentActorRef ! GetAgentStatus(probe.ref)

      //ensure both components are initializing
      probe.expectMessage(AgentStatus(Map(componentId1 -> Initializing, componentId2 -> Initializing)))
    }
  }
}
