package esw.sm.app

import java.nio.file.Path

import akka.actor.CoordinatedShutdown.UnknownReason
import caseapp.RemainingArgs
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.client.utils.LocationServerStatus
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.agent.akka.app.{AgentApp, AgentSettings}
import esw.commons.cli.EswCommandApp
import esw.constants.CommonTimeouts
import esw.sm.app.SequenceManagerAppCommand._
import esw.sm.app.utils.ResourceReader

import scala.concurrent.Await
import scala.util.control.NonFatal

// $COVERAGE-OFF$
object SequenceManagerApp extends EswCommandApp[SequenceManagerAppCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version

  override def progName: String = BuildInfo.name

  override def run(command: SequenceManagerAppCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    run(command)
  }

  def run(command: SequenceManagerAppCommand, startLogging: Boolean = true): SequenceManagerWiring =
    command match {
      case StartCommand(obsModeConfigPath, isConfigLocal, agentPrefix, simulation) => {
        if (simulation) {
          lazy val defaultConfPath = ResourceReader.copyToTmp("smSimulationObsMode.conf").getAbsolutePath
          lazy val configPath      = obsModeConfigPath.getOrElse(Path.of(defaultConfPath))
          start(configPath, isConfigLocal = true, agentPrefix, startLogging, simulation)
        }
        else
          start(obsModeConfigPath.get, isConfigLocal, agentPrefix, startLogging, simulation)
      }
    }

  def start(
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      agentPrefix: Option[Prefix],
      startLogging: Boolean,
      simulation: Boolean
  ): SequenceManagerWiring = {

    if (simulation) startSimulation()
    val sequenceManagerWiring = new SequenceManagerWiring(obsModeConfigPath, isConfigLocal, agentPrefix, simulation)
    import sequenceManagerWiring._

    try {
      if (startLogging) actorRuntime.startLogging(progName, appVersion)
      logResult(sequenceManagerWiring.start())
      sequenceManagerWiring
    }
    catch {
      case NonFatal(e) =>
        Await.result(actorRuntime.shutdown(UnknownReason), CommonTimeouts.Wiring)
        throw e
    }
  }

  private def spawnAgent(agentPrefix: Prefix, agentConfig: Config): Unit = {
    val agentSettings = AgentSettings(agentPrefix, agentConfig)
    AgentApp.start(agentSettings)
  }

  def startSimulation(): Unit = {
    // TODO this can be removed once the ocs-app changes are brought to master
    lazy val channelConfFile: String = ResourceReader.copyToTmp("apps.json").getAbsolutePath
    lazy val agentConfigS            = s"""
                                                  |agent {
                                                  |  durationToWaitForComponentRegistration = 35s
                                                  |  coursier.channel = "file://${channelConfFile}"
                                                  |}""".stripMargin

    lazy val agentConfig = ConfigFactory.parseString(agentConfigS).withFallback(ConfigFactory.load())
    // TODO upto here
    spawnAgent(Prefix(ESW, "machine1"), agentConfig)
    spawnAgent(Prefix(TCS, "machine1"), agentConfig)
    spawnAgent(Prefix(IRIS, "machine1"), agentConfig)
  }
}
// $COVERAGE-ON$
