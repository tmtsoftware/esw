package esw.services

import akka.actor.CoordinatedShutdown
import caseapp.{Command, RemainingArgs}
import caseapp.core.app.CommandsEntryPoint
import caseapp.core.help.Help
import caseapp.core.parser.Parser
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks
import esw.commons.cli.EswCommand
import esw.services.cli
import esw.services.cli.Command.StartOptions
import esw.services.cli.Command.StartEngUIServicesOptions
import esw.sm.app.SequenceManagerAppCommand.prefixParser

import scala.util.control.NonFatal

//main app to start esw services - e.g., agent, agent service, gateway, sm etc
object EswServicesApp extends CommandsEntryPoint {
  def appName: String           = getClass.getSimpleName.dropRight(1)
  def appVersion: String        = BuildInfo.version
  override def progName: String = BuildInfo.name
  println(s"starting $progName-$appVersion")

  val hostname: String = Networks().hostname

  val Start: Runner[StartOptions]                           = Runner[StartOptions]()
  val StartEngUIServices: Runner[StartEngUIServicesOptions] = Runner[StartEngUIServicesOptions]()

  override def commands: Seq[Command[_]] = List(Start, StartEngUIServices)

  class Runner[T <: cli.Command: Parser: Help] extends EswCommand[T] {
    override def run(command: T, args: RemainingArgs): Unit = {
      val wiring = new Wiring(command)
      try {
        LoggingSystemFactory.start(appName, appVersion, hostname, wiring.actorSystem)
        wiring.start()
        CoordinatedShutdown(wiring.actorSystem).addJvmShutdownHook(wiring.stop())
      }
      catch {
        case NonFatal(e) =>
          e.printStackTrace()
          wiring.stop()
          exit(1)
      }
    }
  }
}
