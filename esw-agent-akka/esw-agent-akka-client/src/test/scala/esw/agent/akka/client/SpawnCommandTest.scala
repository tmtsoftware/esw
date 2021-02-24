package esw.agent.akka.client

import akka.actor.typed.ActorRef
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentCommand.SpawnCommand.{SpawnContainer, SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.akka.client.models.ContainerConfig
import esw.agent.service.api.models.SpawnResponse
import esw.testcommons.BaseTestSuite

import java.nio.file.Path

class SpawnCommandTest extends BaseTestSuite {

  "SpawnSequenceComponent's commandArg method" must {
    "append given extra argument | ESW-366" in {
      val actorRef      = mock[ActorRef[SpawnResponse]]
      val agentPrefix   = Prefix(randomSubsystem, randomString(10))
      val componentName = randomString(10)
      val command       = SpawnSequenceComponent(actorRef, agentPrefix, componentName, None)

      val expectedDefaultArgs = List("seqcomp", "-s", agentPrefix.subsystem.name, "-n", componentName)
      val randomArgs          = List(randomString(10), randomString(10))

      command.commandArgs() should ===(expectedDefaultArgs)
      command.commandArgs(randomArgs) should ===(expectedDefaultArgs ++ randomArgs)
    }

    "append simulation argument | ESW-174" in {
      val actorRef      = mock[ActorRef[SpawnResponse]]
      val agentPrefix   = Prefix(randomSubsystem, randomString(10))
      val componentName = randomString(10)
      val command       = SpawnSequenceComponent(actorRef, agentPrefix, componentName, None, simulation = true)

      val expectedDefaultArgs = List("seqcomp", "-s", agentPrefix.subsystem.name, "-n", componentName, "--simulation")
      command.commandArgs() should ===(expectedDefaultArgs)
    }
  }

  "SpawnSequenceManager's commandArg method" must {
    val actorRef    = mock[ActorRef[SpawnResponse]]
    val obsConfPath = Path.of(randomString(20))

    "append given extra argument | ESW-366" in {
      val command = SpawnSequenceManager(actorRef, obsConfPath, isConfigLocal = false, None)

      val expectedDefaultArgs = List("start", "-o", obsConfPath.toString)
      val randomArgs          = List(randomString(10), randomString(10))

      command.commandArgs() should ===(expectedDefaultArgs)
      command.commandArgs(randomArgs) should ===(expectedDefaultArgs ++ randomArgs)
    }

    "append given simulation argument | ESW-366, ESW-174" in {
      val command = SpawnSequenceManager(actorRef, obsConfPath, isConfigLocal = false, None, simulation = true)

      val expectedDefaultArgs = List("start", "-o", obsConfPath.toString, "--simulation")

      command.commandArgs() should ===(expectedDefaultArgs)
    }

    "append -l if config path is Local" in {
      val command             = SpawnSequenceManager(actorRef, obsConfPath, isConfigLocal = true, None)
      val expectedDefaultArgs = List("start", "-o", obsConfPath.toString, "-l")

      val randomArgs = List(randomString(10), randomString(10))

      command.commandArgs() should ===(expectedDefaultArgs)
      command.commandArgs(randomArgs) should ===(expectedDefaultArgs ++ randomArgs)
    }
  }

  "SpawnContainer's commandArg method" must {
    val actorRef = mock[ActorRef[SpawnResponse]]

    "append given extra argument | ESW-379" in {
      val containerConfig =
        ContainerConfig("org", "dep", "app", "ver", "container", Path.of("container.conf"), isConfigLocal = false)
      val command = SpawnContainer(actorRef, containerConfig)

      val expectedDefaultArgs = List(containerConfig.configFilePath.toString)
      val randomArgs          = List(randomString(10), randomString(10))

      command.commandArgs() should ===(expectedDefaultArgs)
      command.commandArgs(randomArgs) should ===(expectedDefaultArgs ++ randomArgs)
    }

    "append --standalone if mode is Standalone | ESW-379" in {
      val containerConfig =
        ContainerConfig("org", "dep", "app", "ver", "Standalone", Path.of("container.conf"), isConfigLocal = false)
      val command = SpawnContainer(actorRef, containerConfig)

      val expectedDefaultArgs = List("--standalone", containerConfig.configFilePath.toString)
      val randomArgs          = List(randomString(10), randomString(10))

      command.commandArgs() should ===(expectedDefaultArgs)
      command.commandArgs(randomArgs) should ===(expectedDefaultArgs ++ randomArgs)
    }

    "append --local if config path is Local | ESW-379" in {
      val containerConfig =
        ContainerConfig("org", "dep", "app", "ver", "container", Path.of("container.conf"), isConfigLocal = true)
      val command = SpawnContainer(actorRef, containerConfig)

      val expectedDefaultArgs = List("--local", containerConfig.configFilePath.toString)
      val randomArgs          = List(randomString(10), randomString(10))

      command.commandArgs() should ===(expectedDefaultArgs)
      command.commandArgs(randomArgs) should ===(expectedDefaultArgs ++ randomArgs)
    }
  }
}
