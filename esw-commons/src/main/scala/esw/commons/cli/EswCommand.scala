package esw.commons.cli

import caseapp.core.app.Command
import caseapp.core.help.Help
import caseapp.core.parser.Parser
import caseapp.core
import csw.location.api.models.PekkoLocation
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.GenericLoggerFactory

// $COVERAGE-OFF$

/**
 * This is a base class for all ESW command line applications containing helper utilities.
 */
abstract class EswCommand[T: Parser: Help] extends Command[T] {

  lazy val log: Logger = GenericLoggerFactory.getLogger

//  override def helpAsked(): Nothing = {
//    help()
//    exit(0)
//  }

  override def error(message: core.Error): Nothing = {
    colored(Console.RED, message.message)
    println()
    println(finalHelp.help(helpFormat, showHidden = true))
    exit(255)
  }

  def logAndThrowError(log: Logger, msg: String, err: Exception): Nothing = {
    log.error(msg)
    colored(Console.RED, msg)
    throw err
  }

  inline def logInfo(log: Logger, msg: String): Unit = {
    log.info(msg)
    println(msg)
  }

  private def colored(color: String, msg: String): Unit = println(s"$color$msg${Console.RESET}")

//  private def help(): Unit = {
//    print(beforeCommandMessages.withOptionsDesc(s"[command] [command-options]").help)
//    println(s"Available commands: ${commands.mkString(", ")}\n")
//    println(s"Type  $progName command --help  for help on an individual command")
//  }

  def logResult(appResult: Either[Exception, PekkoLocation]): PekkoLocation =
    appResult match {
      case Left(err) => logAndThrowError(log, s"Failed to start with error: $err", err)
      case Right(location) =>
        logInfo(
          log,
          s"Successfully started and registered ${location.connection.componentId.componentType} with Location: [$location]"
        )
        location
    }

}
// $COVERAGE-ON$
