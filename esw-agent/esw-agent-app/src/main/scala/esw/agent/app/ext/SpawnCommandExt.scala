package esw.agent.app.ext

import esw.agent.api.AgentCommand.SpawnCommand
import esw.agent.api.AgentCommand.SpawnCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.api.AgentCommand.SpawnCommand.SpawnSelfRegistered.{SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.app.process.cs.Coursier
import esw.agent.app.process.redis.Redis

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
