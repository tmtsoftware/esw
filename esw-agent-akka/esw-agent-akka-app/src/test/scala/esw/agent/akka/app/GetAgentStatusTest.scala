package esw.agent.akka.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models._
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentCommand.GetAgentStatus
import esw.agent.akka.client.AgentCommand.SpawnCommand.SpawnSelfRegistered.SpawnSequenceComponent
import esw.agent.service.api.models.ComponentStatus.Initializing
import esw.agent.service.api.models.{AgentStatus, SpawnResponse}
import org.mockito.ArgumentMatchers.any
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper

import scala.concurrent.duration.{DurationLong, FiniteDuration}

class GetAgentStatusTest extends AgentSetup {

  "GetAgentStatus" must {

    "reply with a collection of status of all components available on the agent | ESW-286" in {
      val agentPrefix       = Prefix(randomSubsystem, randomString(10))
      val seqComponentName1 = randomString(10)
      val prefix1           = Prefix(agentPrefix.subsystem, seqComponentName1)
      val componentId1      = ComponentId(prefix1, SequenceComponent)

      val seqComponentName2 = randomString(10)
      val prefix2           = Prefix(agentPrefix.subsystem, seqComponentName2)
      val componentId2      = ComponentId(prefix2, SequenceComponent)

      val agentActorRef = spawnAgentActor()
      val spawner       = TestProbe[SpawnResponse]()
      val probe         = TestProbe[AgentStatus]()

      when(locationService.resolve(any[TypedConnection[AkkaLocation]], any[FiniteDuration]))
        .thenReturn(delayedFuture(None, 2.seconds))

      //spawn two processes
      agentActorRef ! SpawnSequenceComponent(spawner.ref, agentPrefix, seqComponentName1, None)
      agentActorRef ! SpawnSequenceComponent(spawner.ref, agentPrefix, seqComponentName2, None)

      //get agent status
      agentActorRef ! GetAgentStatus(probe.ref)

      //ensure both components are initializing
      probe.expectMessage(AgentStatus(Map(componentId1 -> Initializing, componentId2 -> Initializing)))
    }
  }
}
