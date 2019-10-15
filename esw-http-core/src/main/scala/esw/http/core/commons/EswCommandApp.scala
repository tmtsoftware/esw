package esw.http.core.commons

import caseapp.core
import caseapp.core.app.CommandApp
import caseapp.core.commandparser.CommandParser
import caseapp.core.help.CommandsHelp
import csw.logging.api.scaladsl.Logger

abstract class EswCommandApp[T](implicit commandParser: CommandParser[T], commandsMessages: CommandsHelp[T])
    extends CommandApp[T] {

  override def error(message: core.Error): Nothing = {
    println(message.message)
    print(beforeCommandMessages.help)
    println(s"Available commands: ${commands.mkString(", ")}\n")
    println(s"Type  $progName command --help  for help on an individual command")
    exit(255)
  }

  def logAndThrowError(log: Logger, msg: String) = {
    log.error(msg)
    println(s"[ERROR] $msg")
    throw new RuntimeException(msg)
  }

  def logInfo(log: Logger, msg: String): Unit = {
    log.info(msg)
    println(s"[INFO] $msg")
  }
}
