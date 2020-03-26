package esw.http.core.commons

import caseapp.core
import caseapp.core.app.CommandApp
import caseapp.core.commandparser.CommandParser
import caseapp.core.help.CommandsHelp
import csw.logging.api.scaladsl.Logger

// $COVERAGE-OFF$
abstract class EswCommandApp[T: CommandParser: CommandsHelp] extends CommandApp[T] {

  override def helpAsked(): Nothing = {
    help()
    exit(0)
  }

  override def error(message: core.Error): Nothing = {
    colored(Console.RED, message.message)
    println()
    help()
    exit(255)
  }

  def logAndThrowError(log: Logger, msg: String): Nothing = {
    log.error(msg)
    colored(Console.RED, msg)
    throw new RuntimeException(msg)
  }

  def logInfo(log: Logger, msg: String): Unit = {
    log.info(msg)
    println(msg)
  }

  private def colored(color: String, msg: String): Unit = println(s"$color$msg${Console.RESET}")

  private def help(): Unit = {
    print(beforeCommandMessages.withOptionsDesc(s"[command] [command-options]").help)
    println(s"Available commands: ${commands.mkString(", ")}\n")
    println(s"Type  $progName command --help  for help on an individual command")
  }

}
// $COVERAGE-ON$
