package esw.agent.akka.app.ext

import java.nio.file.Path

import csw.config.client.commons.ConfigUtils
import csw.prefix.models.Prefix
import esw.agent.akka.app.process.cs.Coursier
import esw.agent.akka.client.AgentCommand.SpawnCommand
import esw.agent.akka.client.AgentCommand.SpawnCommand.{SpawnSequenceComponent, SpawnSequenceManager}

import scala.concurrent.{ExecutionContext, Future}

object SpawnCommandExt {

  implicit class SpawnCommandOps(private val command: SpawnCommand) extends AnyVal {

    //returns a list of command strings which can be executed on the command line(e.g., bash etc)
    def executableCommandStr(coursierChannel: String, agentPrefix: Prefix, configUtils: ConfigUtils, versionConfPath: Path)(
        implicit ec: ExecutionContext
    ): Future[List[String]] = {
      lazy val args = command.commandArgs(List("-a", agentPrefix.toString()))

      def findVersion(path: Path): Future[Option[String]] =
        configUtils
          .getConfig(path, isLocal = false)
          .map { config =>
            try Some(config.getString("scripts.version"))
            catch {
              case _: Throwable => None
            }
          }

      command match {
        case SpawnSequenceComponent(_, _, _, version) =>
          val scriptsVersion = if (version.isEmpty) findVersion(versionConfPath) else Future.successful(version)
          scriptsVersion.map(Coursier.ocsApp(_).launch(coursierChannel, args))
        case SpawnSequenceManager(_, _, _, version) => Future.successful(Coursier.smApp(version).launch(coursierChannel, args))
      }
    }
  }

}
