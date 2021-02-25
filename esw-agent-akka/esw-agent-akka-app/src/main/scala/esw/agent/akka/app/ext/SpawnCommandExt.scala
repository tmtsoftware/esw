package esw.agent.akka.app.ext

import esw.agent.akka.app.AgentSettings
import esw.agent.akka.app.process.cs.Coursier
import esw.agent.akka.client.AgentCommand.SpawnCommand
import esw.agent.akka.client.AgentCommand.SpawnCommand.{SpawnContainer, SpawnSequenceComponent, SpawnSequenceManager}
import esw.commons.utils.config.VersionManager

import scala.concurrent.{ExecutionContext, Future}

object SpawnCommandExt {

  implicit class SpawnCommandOps(private val command: SpawnCommand) extends AnyVal {

    //returns a list of command strings which can be executed on the command line(e.g., bash etc)
    def executableCommandStr(agentSettings: AgentSettings, versionManager: VersionManager)(implicit
        ec: ExecutionContext
    ): Future[List[String]] = {
      lazy val args = command.commandArgs(List("-a", agentSettings.prefix.toString()))

      command match {
        case SpawnSequenceComponent(_, _, _, version, _) =>
          val scriptsVersion =
            if (version.isEmpty) versionManager.getScriptVersion(agentSettings.versionConfPath).map(Some(_))
            else Future.successful(version)
          scriptsVersion.map(Coursier.ocsApp(_, agentSettings.gcMetricsEnabled).launch(agentSettings.coursierChannel, args))
        case SpawnSequenceManager(_, _, _, version, _) =>
          Future.successful(Coursier.smApp(version, agentSettings.gcMetricsEnabled).launch(agentSettings.coursierChannel, args))
        case SpawnContainer(_, _, config) =>
          Future.successful(
            Coursier
              .containerApp(config, agentSettings.gcMetricsEnabled)
              .launch(List("jitpack"), config.appName, command.commandArgs())
          )
      }
    }
  }

}
