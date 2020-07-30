package esw.agent.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.SpawnCommand.SpawnSelfRegistered.SpawnSequenceComponent
import esw.agent.api._
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class SpawnSelfRegisteredComponentTest extends AgentSetup {

  "SpawnSelfRegistered" must {
    "reply 'Spawned' and spawn component process | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor1")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(Future.successful(None), seqCompLocationF)

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, seqCompPrefix)
      probe.expectMessage(Spawned)
    }

    "reply 'Failed' and not spawn new process when call to location service fails" in {
      val agentActorRef = spawnAgentActor(name = "test-actor2")
      val probe         = TestProbe[SpawnResponse]()
      val err           = "Failed to resolve component"
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(Future.failed(new RuntimeException(err)))

      agentActorRef ! SpawnSequenceComponent(probe.ref, seqCompPrefix)
      probe.expectMessage(Failed(s"Failed to verify component registration in location service, reason: $err"))
    }

    "reply 'Failed' and not spawn new process when it is already registered with location service | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor3")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(seqCompLocationF)

      agentActorRef ! SpawnSequenceComponent(probe.ref, seqCompPrefix)
      probe.expectMessage(
        Failed(
          s"Component ${seqCompComponentId.fullName} is already registered with location service at location $seqCompLocation"
        )
      )
    }

    "reply 'Failed' and not spawn new process when it is already spawned on the agent | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor4")
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(Future.successful(None), seqCompLocationF)

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe1.ref, seqCompPrefix)
      agentActorRef ! SpawnSequenceComponent(probe2.ref, seqCompPrefix)

      probe1.expectMessage(Spawned)
      probe2.expectMessage(Failed(s"Component ${seqCompComponentId.fullName} is already running on this agent"))
    }

    "reply 'Failed' when process fails to spawn | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor5")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(Future.successful(None), seqCompLocationF)
      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Left("failure"))

      agentActorRef ! SpawnSequenceComponent(probe.ref, seqCompPrefix)
      probe.expectMessage(Failed("failure"))
    }

    "reply 'Failed' and kill process, when the process is spawned but failed to register | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor6")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(Future.successful(None))

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, seqCompPrefix)
      probe.expectMessage(Failed(s"Component ${seqCompComponentId.fullName} is not registered with location service"))
    }
  }
}
