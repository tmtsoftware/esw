package esw.agent.pekko.app.ext

import csw.prefix.models.Prefix
import esw.agent.pekko.app.process.cs.Coursier
import esw.agent.pekko.client.AgentCommand.SpawnCommand
import esw.agent.pekko.client.AgentCommand.SpawnCommand.{SpawnContainer, SpawnSequenceComponent, SpawnSequenceManager}
import esw.commons.utils.config.VersionManager

import scala.concurrent.{ExecutionContext, Future}

/**
 * This is a convenience utility which can be used to spawn ESW components via Coursier.
 */
object SpawnCommandExt {

  implicit class SpawnCommandOps(private val command: SpawnCommand) extends AnyVal {

    // returns a list of command strings which can be executed on the command line(e.g., bash etc)
    def executableCommandStr(coursierChannel: String, agentPrefix: Prefix, versionManager: VersionManager)(implicit
        ec: ExecutionContext
    ): Future[List[String]] = {
      lazy val args = command.commandArgs(List("-a", agentPrefix.toString()))

      command match {
        case SpawnSequenceComponent(_, _, _, version, _) =>
          val scriptsVersion =
            if (version.isEmpty) versionManager.getScriptVersion.map(Some(_))
            else Future.successful(version)
          scriptsVersion.map(Coursier.ocsApp(_).launch(coursierChannel, args))
        case SpawnSequenceManager(_, _, _, version, _) =>
          val smVersion =
            if (version.isEmpty) versionManager.eswVersion.map(Some(_))
            else Future.successful(version)
          smVersion.map(Coursier.smApp(_).launch(coursierChannel, args))
        case SpawnContainer(_, _, config) =>
          Future.successful(Coursier.containerApp(config).launch(List("jitpack"), config.appName, command.commandArgs()))
      }
    }
  }

}
