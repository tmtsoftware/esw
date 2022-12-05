package esw.agent.akka.app

import caseapp.core.Error
import caseapp.core.argparser.SimpleArgParser
import caseapp.{CommandName, HelpMessage, ExtraName => Short}
import csw.prefix.models.Prefix

import scala.util.Try

sealed trait AgentCliCommand

/**
 * AgentCliCommand - a set of command line param written using case app for the Agent App.
 */
object AgentCliCommand {

  implicit val prefixParser: SimpleArgParser[Prefix] =
    SimpleArgParser.from[Prefix]("prefix") { prefixStr =>
      Try(Right(Prefix(prefixStr)))
        .getOrElse(Left(Error.Other(s"Prefix [$prefixStr] is invalid")))
    }

  final case class StartOptions(
      @HelpMessage("Required: Prefix of machine. tcs.primary_machine, esw.machine1 etc")
      @Short("p")
      prefix: String,
      @HelpMessage("Optional: Path of host config for this agent")
      @Short("h")
      hostConfigPath: Option[String],
      @HelpMessage("Optional: Flag for reading host config from local machine")
      @Short("l")
      local: Boolean = false
  ) extends AgentCliCommand
}
