package esw.agent.akka.app.ext

import csw.prefix.models.Prefix
import esw.agent.akka.app.process.cs.Coursier
import esw.agent.akka.client.AgentCommand.SpawnCommand
import esw.agent.akka.client.AgentCommand.SpawnCommand.{SpawnPostgres, SpawnRedis, SpawnSequenceComponent, SpawnSequenceManager}

object SpawnCommandExt {

  implicit class SpawnCommandOps(private val command: SpawnCommand) extends AnyVal {

    //returns a list of command strings which can be executed on the command line(e.g., bash etc)
    def executableCommandStr(coursierChannel: String, agentPrefix: Prefix): List[String] = {
      lazy val args = command.commandArgs(List("-a", agentPrefix.toString()))

      command match {
        case SpawnSequenceComponent(_, _, _, version) =>
          Coursier.ocsApp(version).launch(coursierChannel, args)
        case SpawnSequenceManager(_, _, _, version) => Coursier.smApp(version).launch(coursierChannel, args)
        case SpawnRedis(_, _, _, _, version)        => Coursier.locationAgentApp(version).launch(coursierChannel, args)
        case SpawnPostgres(_, _, _, _, _, version)  => Coursier.locationAgentApp(version).launch(coursierChannel, args)
      }
    }
  }

}
