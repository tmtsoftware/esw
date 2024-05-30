package esw.sm.app

import org.apache.pekko.actor.CoordinatedShutdown.UnknownReason
import caseapp.{Command, RemainingArgs}
import caseapp.core.app.CommandsEntryPoint
import caseapp.core.help.Help
import caseapp.core.parser.Parser
import csw.location.client.utils.LocationServerStatus
import csw.prefix.models.Prefix
import esw.commons.cli.EswCommand
import esw.commons.utils.files.FileUtils
import esw.constants.CommonTimeouts
import esw.sm.app.SequenceManagerAppCommand.*

import java.nio.file.Path
import scala.concurrent.Await
import scala.util.control.NonFatal

/*
 * The main app to start Sequence Manager
 */
// $COVERAGE-OFF$
object SequenceManagerApp extends CommandsEntryPoint {
  def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  def appVersion: String = BuildInfo.version

  override def progName: String = BuildInfo.name

  val StartCommand: Runner[StartOptions] = Runner[StartOptions]()

  override def commands: Seq[Command[?]] = List(StartCommand)

  class Runner[T <: SequenceManagerAppCommand: Parser: Help] extends EswCommand[T] {
    override def run(command: T, args: RemainingArgs): Unit = {
      LocationServerStatus.requireUpLocally()
      run(command)
    }

    def run(command: SequenceManagerAppCommand, startLogging: Boolean = true): SequenceManagerWiring =
      command match {
        case StartOptions(port, obsModeConfigPath, isConfigLocal, agentPrefix, simulation) => {
          if (simulation) {
            lazy val defaultConfPath = FileUtils.cpyFileToTmpFromResource("smSimulationObsMode.conf")
            lazy val configPath      = obsModeConfigPath.getOrElse(defaultConfPath)
            start(port, configPath, isConfigLocal = true, agentPrefix, startLogging, simulation)
          }
          else
            obsModeConfigPath match {
              case Some(path) => start(port, path, isConfigLocal, agentPrefix, startLogging, simulation)
              case None       => throw new IllegalArgumentException("obsMode config file path must be provided")
            }
        }
      }

    def start(
        port: Option[Int],
        obsModeConfigPath: Path,
        isConfigLocal: Boolean,
        agentPrefix: Option[Prefix],
        startLogging: Boolean,
        simulation: Boolean
    ): SequenceManagerWiring = {

      val sequenceManagerWiring = new SequenceManagerWiring(port, obsModeConfigPath, isConfigLocal, agentPrefix, simulation)
      import sequenceManagerWiring.*

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
  }

}
// $COVERAGE-ON$
