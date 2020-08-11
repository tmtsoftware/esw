package esw.agent.akka.app.ext

import esw.agent.akka.app.process.cs.Coursier
import esw.agent.akka.app.process.redis.Redis
import esw.agent.akka.client.AgentCommand.SpawnCommand
import esw.agent.akka.client.AgentCommand.SpawnCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.akka.client.AgentCommand.SpawnCommand.SpawnSelfRegistered.{SpawnSequenceComponent, SpawnSequenceManager}

object SpawnCommandExt {

  implicit class SpawnCommandOps(private val command: SpawnCommand) extends AnyVal {
    def executableCommandStr(coursierChannel: String): List[String] =
      command match {
        case SpawnSequenceComponent(_, _, version)  => Coursier.ocsApp(version).launch(coursierChannel, command.commandArgs)
        case SpawnSequenceManager(_, _, _, version) => Coursier.smApp(version).launch(coursierChannel, command.commandArgs)
        case _: SpawnRedis                          => Redis.server :: command.commandArgs
      }
  }

}
