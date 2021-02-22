package esw.agent.akka.app

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.prefix.models.Prefix
import esw.agent.akka.app.process.cs.Coursier
import esw.agent.akka.client.AgentCommand.SpawnCommand.{SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.akka.client.AgentCommand.StartContainers
import esw.agent.service.api.models.{Failed, SpawnResponse, Spawned, StartContainersResponse}
import esw.commons.utils.config.FetchingScriptVersionFailed
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper

import java.nio.file.{Path, Paths}
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class SpawnComponentTest extends AgentSetup {

  "Spawn" must {
    "reply 'Spawned' and spawn sequence component process | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor1")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(seqCompConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(seqCompLocationF)

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(Spawned)

      val expectedCommand =
        List(
          Coursier.cs,
          "launch",
          "--channel",
          Cs.channel,
          s"ocs-app:$sequencerScriptsVersion",
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

    "reply 'Spawned' and spawn sequence component process in simulation mode | ESW-174" in {
      val agentActorRef = spawnAgentActor(name = "test-actor-random1")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(seqCompConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(seqCompLocationF)

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None, simulation = true)
      probe.expectMessage(Spawned)

      val expectedCommand =
        List(
          Coursier.cs,
          "launch",
          "--channel",
          Cs.channel,
          s"ocs-app:$sequencerScriptsVersion",
          "--",
          "seqcomp",
          "-s",
          agentPrefix.subsystem.name,
          "-n",
          seqCompName,
          "-a",
          agentPrefix.toString(),
          "--simulation"
        )
      verify(processExecutor).runCommand(expectedCommand, seqCompPrefix)
    }

    "reply 'Failed' and not spawn new process when call to location service fails" in {
      val agentActorRef = spawnAgentActor(name = "test-actor2")
      val probe         = TestProbe[SpawnResponse]()
      val err           = "Failed to resolve component"
      when(locationService.find(argEq(seqCompConn))).thenReturn(Future.failed(new RuntimeException(err)))

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(Failed(s"Failed to verify component registration in location service, reason: $err"))
    }

    "reply 'Failed' and not spawn new process when it is already registered with location service | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor3")
      val probe         = TestProbe[SpawnResponse]()
      when(locationService.find(argEq(seqCompConn))).thenReturn(seqCompLocationF)

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(Failed(s"$seqCompComponentId is already registered with location service at $seqCompLocation"))
    }

    "reply 'Failed' when process fails to spawn | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor5")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(seqCompConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(seqCompLocationF)
      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Left("failure"))

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(Failed("failure"))
    }

    "reply 'Failed' and kill process, when the process is spawned but failed to register | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor6")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(seqCompConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(Future.successful(None))

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(
        Failed(
          s"$seqCompComponentId is not registered with location service. Reason: Process failed to spawn due to reasons like invalid binary version etc or failed to register with location service."
        )
      )
    }

    "reply 'Failed' if process fails before registration is successful | ESW-367" in {
      val agentActorRef = spawnAgentActor(name = "test-actor10")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(seqCompConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(seqCompLocationF)

      mockSuccessfulProcess()
      when(process.isAlive).thenReturn(false)
      when(locationService.unregister(seqCompConn)).thenReturn(Future.successful(Done))

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)
      probe.expectMessage(Failed("Process terminated before registration was successful"))
    }

    "reply 'Spawned' and spawn sequence manager process | ESW-180" in {
      val agentActorRef = spawnAgentActor(name = "test-actor9")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(seqManagerConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqManagerConn), any[FiniteDuration])).thenReturn(seqManagerLocationF)

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

    "reply 'Spawned' and spawn sequence manager process in simulation mode | ESW-174" in {
      val agentActorRef = spawnAgentActor(name = "test-actor-random-9")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(seqManagerConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqManagerConn), any[FiniteDuration])).thenReturn(seqManagerLocationF)

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceManager(probe.ref, Path.of("obsMode.conf"), isConfigLocal = true, None, simulation = true)
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
          agentPrefix.toString(),
          "--simulation"
        )

      verify(processExecutor).runCommand(expectedCommand, seqManagerPrefix)
    }

    "reply 'Failed' if ScriptVersionConfException occurred | ESW-360" in {
      val agentActorRef = spawnAgentActor(name = "test-actor-scriptVersionConfException")
      val probe         = TestProbe[SpawnResponse]()
      val errorMsg      = randomString(10)

      when(locationService.find(argEq(seqCompConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(seqCompLocationF)
      when(versionManager.getScriptVersion(versionConfPath)).thenReturn(Future.failed(FetchingScriptVersionFailed(errorMsg)))

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None)

      probe.expectMessage(Failed(errorMsg))
    }

    "StartContainers" must {
      val hostConfigPath: Path = Paths.get(getClass.getResource("/hostConfig.conf").toURI)
      val isHostConfigLocal    = true
      def expectedCommand(org: String, module: String, appName: String, confPath: String) =
        List(
          "cs",
          "launch",
          s"$org::$module:0.0.1",
          "-r",
          "jitpack",
          "-M",
          s"$appName",
          "--",
          "--local",
          s"$confPath"
        )

      "reply 'StartContainersResponse' and spawn containers on agent spawn | ESW-379" in {
        when(configUtils.getConfig(hostConfigPath, isHostConfigLocal))
          .thenReturn(Future.successful(ConfigFactory.parseFile(hostConfigPath.toFile)))
        mockLocationService()
        mockSuccessfulProcess()

        spawnAgentActor(agentSettings, "test-actor-random-10", Some(hostConfigPath), isHostConfigLocal)

        // agent actor sends StartContainers message to self
        Thread.sleep(100)

        verify(processExecutor).runCommand(
          expectedCommand("com.github.tmtsoftware.sample", "csw-sampledeploy", "SampleContainerCmdApp", "confPath.conf"),
          firstContainerPrefix
        )
        verify(processExecutor).runCommand(
          expectedCommand("com.github.tmtsoftware.sample2", "csw-sampledeploy2", "SampleContainerCmdApp2", "confPath2.conf"),
          secondContainerPrefix
        )
      }

      "reply 'StartContainersResponse' and spawn containers on receiving message | ESW-379" in {
        LoggingSystemFactory.forTestingOnly()
        val agentActorRef = spawnAgentActor(name = "test-actor-random-11")
        val probe         = TestProbe[StartContainersResponse]()

        when(configUtils.getConfig(hostConfigPath, isHostConfigLocal))
          .thenReturn(Future.successful(ConfigFactory.parseFile(hostConfigPath.toFile)))
        mockLocationService()
        mockSuccessfulProcess()

        agentActorRef ! StartContainers(probe.ref, hostConfigPath, isHostConfigLocal)
        probe.expectMessage(StartContainersResponse(List(Spawned, Spawned)))

        verify(processExecutor).runCommand(
          expectedCommand("com.github.tmtsoftware.sample", "csw-sampledeploy", "SampleContainerCmdApp", "confPath.conf"),
          firstContainerPrefix
        )
        verify(processExecutor).runCommand(
          expectedCommand("com.github.tmtsoftware.sample2", "csw-sampledeploy2", "SampleContainerCmdApp2", "confPath2.conf"),
          secondContainerPrefix
        )
      }
    }
  }
}
