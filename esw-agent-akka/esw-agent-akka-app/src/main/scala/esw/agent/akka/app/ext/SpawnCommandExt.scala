package esw.agent.akka.app.ext

import csw.prefix.models.Prefix
import esw.agent.akka.app.process.cs.Coursier
import esw.agent.akka.client.AgentCommand.SpawnCommand
import esw.agent.akka.client.AgentCommand.SpawnCommand.{SpawnContainer, SpawnSequenceComponent, SpawnSequenceManager}
import esw.commons.utils.config.VersionManager

import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future}

object SpawnCommandExt {

  implicit class SpawnCommandOps(private val command: SpawnCommand) extends AnyVal {

    //returns a list of command strings which can be executed on the command line(e.g., bash etc)
    def executableCommandStr(coursierChannel: String, agentPrefix: Prefix, versionManager: VersionManager, versionConfPath: Path)(
        implicit ec: ExecutionContext
    ): Future[List[String]] = {
      lazy val args = command.commandArgs(List("-a", agentPrefix.toString()))

      command match {
        case SpawnSequenceComponent(_, _, _, version, _) =>
          val scriptsVersion =
            if (version.isEmpty) versionManager.getScriptVersion(versionConfPath).map(Some(_)) else Future.successful(version)
          scriptsVersion.map(Coursier.ocsApp(_).launch(coursierChannel, args))
        case SpawnSequenceManager(_, _, _, version, _) => Future.successful(Coursier.smApp(version).launch(coursierChannel, args))
        case SpawnContainer(_, config) =>
          Future.successful(Coursier.containerApp(config).launch(List("jitpack"), config.appName, command.commandArgs()))
      }
    }
  }

}
