package esw.agent.pekko.app

import org.apache.pekko.Done
import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.pekko.app.process.cs.Coursier
import esw.agent.pekko.client.AgentCommand.SpawnCommand.{SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.pekko.client.AgentCommand.SpawnContainers
import esw.agent.service.api.models.*
import esw.commons.utils.config.FetchingScriptVersionFailed
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.must.Matchers.*

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

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None, simulation = false)
      probe.expectMessage(Spawned)

      val expectedCommand =
        List(
          Coursier.cs,
          "launch",
          "-Dtest.esw=true",
          "--channel",
          Cs.channel,
          s"esw-ocs-app:$sequencerScriptsVersion",
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
          "-Dtest.esw=true",
          "--channel",
          Cs.channel,
          s"esw-ocs-app:$sequencerScriptsVersion",
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

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None, simulation = false)
      probe.expectMessage(Failed(s"Failed to verify component registration in location service, reason: $err"))
    }

    "reply 'Failed' and not spawn new process when it is already registered with location service | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor3")
      val probe         = TestProbe[SpawnResponse]()
      when(locationService.find(argEq(seqCompConn))).thenReturn(seqCompLocationF)

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None, simulation = false)
      probe.expectMessage(Failed(s"$seqCompComponentId is already registered with location service at $seqCompLocation"))
    }

    "reply 'Failed' when process fails to spawn | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor5")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(seqCompConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(seqCompLocationF)
      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Left("failure"))

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None, simulation = false)
      probe.expectMessage(Failed("failure"))
    }

    "reply 'Failed' and kill process, when the process is spawned but failed to register | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor6")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.find(argEq(seqCompConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(Future.successful(None))

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None, simulation = false)
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

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None, simulation = false)
      probe.expectMessage(Failed("Process terminated before registration was successful"))
    }

    "reply 'Spawned' and spawn sequence manager process | ESW-180" in {
      val agentActorRef = spawnAgentActor(name = "test-actor9")
      val probe         = TestProbe[SpawnResponse]()

      when(versionManager.eswVersion).thenReturn(Future.successful(eswVersion))
      when(locationService.find(argEq(seqManagerConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqManagerConn), any[FiniteDuration])).thenReturn(seqManagerLocationF)

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceManager(probe.ref, Path.of("obsMode.conf"), isConfigLocal = true, None)
      probe.expectMessage(Spawned)

      val expectedCommand =
        List(
          Coursier.cs,
          "launch",
          "-Dtest.esw=true",
          "--channel",
          Cs.channel,
          s"esw-sm-app:$eswVersion",
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

      when(versionManager.eswVersion).thenReturn(Future.successful(eswVersion))
      when(locationService.find(argEq(seqManagerConn))).thenReturn(Future.successful(None))
      when(locationService.resolve(argEq(seqManagerConn), any[FiniteDuration])).thenReturn(seqManagerLocationF)

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceManager(probe.ref, Path.of("obsMode.conf"), isConfigLocal = true, None, simulation = true)
      probe.expectMessage(Spawned)

      val expectedCommand =
        List(
          Coursier.cs,
          "launch",
          "-Dtest.esw=true",
          "--channel",
          Cs.channel,
          s"esw-sm-app:$eswVersion",
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
      when(versionManager.getScriptVersion).thenReturn(Future.failed(FetchingScriptVersionFailed(errorMsg)))

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, agentPrefix, seqCompName, None, simulation = false)

      probe.expectMessage(Failed(errorMsg))
    }

    "SpawnContainers" must {
      val hostConfigPath: Path = Paths.get(getClass.getResource("/hostConfig.conf").toURI)
      val isHostConfigLocal    = true
      def command(org: String, module: String, appName: String, confPath: String) =
        List(
          "cs",
          "launch",
          "-Dtest.esw=true",
          s"$org:${module}_3:0.0.1",
          "-r",
          "jitpack",
          "-M",
          s"$appName",
          "--",
          "--local",
          s"$confPath"
        )

      "reply 'SpawnContainersResponse' and spawn containers on agent spawn | ESW-379, ESW-584" in {
        when(configUtils.getConfig(hostConfigPath, isHostConfigLocal))
          .thenReturn(Future.successful(ConfigFactory.parseFile(hostConfigPath.toFile)))
        when(configUtils.getConfig(Paths.get("confPath1.conf"), isLocal = true))
          .thenReturn(Future.successful(ConfigFactory.parseString("name = \"testContainer1\"\ncomponents = []")))
        when(configUtils.getConfig(Paths.get("confPath2.conf"), isLocal = true))
          .thenReturn(Future.successful(ConfigFactory.parseString("prefix = \"ESW.testHCD\"\ncomponentType = hcd")))
        mockLocationService()
        mockSuccessfulProcess()

        spawnAgentActor(agentSettings, "test-actor-random-10", Some(hostConfigPath.toString), isHostConfigLocal)

        // agent actor sends SpawnContainers message to self which takes some milliseconds
        eventually {
          verify(processExecutor).runCommand(
            command(
              "com.github.tmtsoftware.sample",
              "csw-sampledeploy",
              "csw.sampledeploy.SampleContainerCmdApp",
              "confPath1.conf"
            ),
            containerPrefixOne
          )
        }

        eventually {
          verify(processExecutor).runCommand(
            List(
              "cs",
              "launch",
              "-Dtest.esw=true",
              "com.github.tmtsoftware.sample2:csw-sample2deploy_3:0.0.1",
              "-r",
              "jitpack",
              "-M",
              "csw.sample2deploy.Sample2ContainerCmdApp",
              "--",
              "--local",
              "confPath2.conf"
            ),
            componentPrefixTwo
          )
        }
      }

      "reply 'Completed' and spawn containers on receiving message | ESW-379, ESW-584" in {
        val agentActorRef = spawnAgentActor(name = "test-actor-random-11")
        val probe         = TestProbe[SpawnContainersResponse]()
        when(configUtils.getConfig(hostConfigPath, isHostConfigLocal))
          .thenReturn(Future.successful(ConfigFactory.parseFile(hostConfigPath.toFile)))
        when(configUtils.getConfig(Paths.get("confPath1.conf"), isLocal = true))
          .thenReturn(Future.successful(ConfigFactory.parseString("name = \"testContainer1\"\ncomponents = []")))
        when(configUtils.getConfig(Paths.get("confPath2.conf"), isLocal = true))
          .thenReturn(Future.successful(ConfigFactory.parseString("prefix = \"ESW.testHCD\"\ncomponentType = HCD")))
        mockLocationService()
        mockSuccessfulProcess()

        agentActorRef ! SpawnContainers(probe.ref, hostConfigPath.toString, isHostConfigLocal)

        val expectedResponse = Completed(
          Map(
            "Container.testContainer1" -> Spawned,
            "ESW.testHCD"              -> Spawned
          )
        )
        probe.expectMessage(expectedResponse)
        verify(processExecutor).runCommand(
          command(
            "com.github.tmtsoftware.sample",
            "csw-sampledeploy",
            "csw.sampledeploy.SampleContainerCmdApp",
            "confPath1.conf"
          ),
          containerPrefixOne
        )
        verify(processExecutor).runCommand(
          List(
            "cs",
            "launch",
            "-Dtest.esw=true",
            "com.github.tmtsoftware.sample2:csw-sample2deploy_3:0.0.1",
            "-r",
            "jitpack",
            "-M",
            "csw.sample2deploy.Sample2ContainerCmdApp",
            "--",
            "--local",
            "confPath2.conf"
          ),
          componentPrefixTwo
        )
      }

      "reply 'Completed' with Failed response if some container fail to spawn | ESW-379, ESW-584" in {
        val agentActorRef = spawnAgentActor(name = "test-actor-random-12")
        val probe         = TestProbe[SpawnContainersResponse]()
        val secondContainerCommand =
          List(
            "cs",
            "launch",
            "-Dtest.esw=true",
            "com.github.tmtsoftware.sample2:csw-sample2deploy_3:0.0.1",
            "-r",
            "jitpack",
            "-M",
            "csw.sample2deploy.Sample2ContainerCmdApp",
            "--",
            "--local",
            "confPath2.conf"
          )
        when(configUtils.getConfig(hostConfigPath, isHostConfigLocal))
          .thenReturn(Future.successful(ConfigFactory.parseFile(hostConfigPath.toFile)))
        when(configUtils.getConfig(Paths.get("confPath1.conf"), isLocal = true))
          .thenReturn(Future.successful(ConfigFactory.parseString("name = \"testContainer1\"\ncomponents = []")))
        when(configUtils.getConfig(Paths.get("confPath2.conf"), isLocal = true))
          .thenReturn(Future.successful(ConfigFactory.parseString("prefix = \"ESW.testHCD\"\ncomponentType = hcd")))
        mockLocationService()
        mockSuccessfulProcess()
        when(
          processExecutor.runCommand(secondContainerCommand, Prefix(ESW, "testHCD"))
        ).thenReturn(Left("Error"))

        agentActorRef ! SpawnContainers(probe.ref, hostConfigPath.toString, isHostConfigLocal)

        val expectedResponse = Completed(
          Map(
            "Container.testContainer1" -> Spawned,
            "ESW.testHCD"              -> Failed("Error")
          )
        )
        probe.expectMessage(expectedResponse)
      }

      "reply 'Failed' if error occurs while spawning containers | ESW-379" in {
        val agentActorRef = spawnAgentActor(name = "test-actor-random-13")
        val probe         = TestProbe[SpawnContainersResponse]()
        when(configUtils.getConfig(hostConfigPath, isHostConfigLocal))
          .thenReturn(Future.failed(new RuntimeException("error")))

        agentActorRef ! SpawnContainers(probe.ref, hostConfigPath.toString, isHostConfigLocal)

        val expectedResponse = Failed("error")
        probe.expectMessage(expectedResponse)
      }
    }
  }
}
