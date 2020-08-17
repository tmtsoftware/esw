package esw.agent.akka.app

import java.nio.file.Path

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.prefix.models.Prefix
import esw.agent.akka.app.process.cs.Coursier
import esw.agent.akka.client.AgentCommand.SpawnCommand.SpawnSelfRegistered.{SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.service.api.models.{Failed, SpawnResponse, Spawned}
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class SpawnSelfRegisteredComponentTest extends AgentSetup {
  "SpawnSelfRegistered" must {
    "reply 'Spawned' and spawn sequence component process | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor1")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(Future.successful(None), seqCompLocationF)

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(Spawned)

      val expectedCommand =
        List(
          Coursier.cs,
          "launch",
          "--channel",
          Cs.channel,
          "ocs-app",
          "--",
          "seqcomp",
          "-s",
          agentPrefix.subsystem.name,
          "-n",
          seqCompName,
          "-a",
          agentPrefix.toString()
        )
      verify(processExecutor).runCommand(expectedCommand, seqCompPrefix)
    }

    "reply 'Failed' and not spawn new process when call to location service fails" in {
      val agentActorRef = spawnAgentActor(name = "test-actor2")
      val probe         = TestProbe[SpawnResponse]()
      val err           = "Failed to resolve component"
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(Future.failed(new RuntimeException(err)))

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(Failed(s"Failed to verify component registration in location service, reason: $err"))
    }

    "reply 'Failed' and not spawn new process when it is already registered with location service | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor3")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(seqCompLocationF)

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
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

      agentActorRef ! SpawnSequenceComponent(probe1.ref, agentPrefix, seqCompName, None)
      agentActorRef ! SpawnSequenceComponent(probe2.ref, agentPrefix, seqCompName, None)

      probe1.expectMessage(Spawned)
      probe2.expectMessage(Failed(s"Component ${seqCompComponentId.fullName} is already running on this agent"))
    }

    "reply 'Failed' when process fails to spawn | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor5")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(Future.successful(None), seqCompLocationF)
      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Left("failure"))

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(Failed("failure"))
    }

    "reply 'Failed' and kill process, when the process is spawned but failed to register | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor6")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(Future.successful(None))

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(Failed(s"Component ${seqCompComponentId.fullName} is not registered with location service"))
    }

    "reply 'Spawned' and spawn sequence manager process | ESW-180" in {
      val agentActorRef = spawnAgentActor(name = "test-actor9")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqManagerConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqManagerLocationF)

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceManager(probe.ref, Path.of("obsMode.conf"), isConfigLocal = true, None)
      probe.expectMessage(Spawned)

      val expectedCommand =
        List(
          Coursier.cs,
          "launch",
          "--channel",
          Cs.channel,
          "sequence-manager",
          "--",
          "start",
          "-o",
          "obsMode.conf",
          "-l",
          "-a",
          agentPrefix.toString()
        )

      verify(processExecutor).runCommand(expectedCommand, seqManagerPrefix)
    }
  }
}
