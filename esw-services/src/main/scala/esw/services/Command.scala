package esw.services

import caseapp.core.Error
import caseapp.core.argparser.SimpleArgParser
import caseapp.{CommandName, ExtraName, HelpMessage}
import csw.prefix.models.Prefix

import java.nio.file.Path
import scala.util.Try

sealed trait Command

object Command {

  implicit val prefixParser: SimpleArgParser[Prefix] =
    SimpleArgParser.from[Prefix]("prefix") { prefixStr =>
      Try(Right(Prefix(prefixStr)))
        .getOrElse(Left(Error.Other(s"Prefix [$prefixStr] is invalid")))
    }

  @CommandName("start")
  final case class Start(
      @ExtraName("a")
      @HelpMessage(
        "start agent"
      )
      agentPrefix: Option[Prefix],
      @ExtraName("g")
      @HelpMessage(
        "start gateway with specified command role mapping file path from local"
      )
      commandRoleConfigPath: Option[Path],
      @ExtraName("s")
      @HelpMessage(
        "start Sequence Manager with specified obsMode config file path from local"
      )
      obsModeConfigPath: Option[Path]
  ) extends Command
}
