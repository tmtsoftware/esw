package esw.sm.app

import java.nio.file.Path

import caseapp.core.Error
import caseapp.core.argparser.SimpleArgParser
import caseapp.{CommandName, ExtraName, HelpMessage}
import csw.prefix.models.Prefix

import scala.util.Try

/**
 * SequenceManagerAppCommand - a set of command line param written using case app for the Sequence Manager
 */
sealed trait SequenceManagerAppCommand

object SequenceManagerAppCommand {

  implicit val prefixParser: SimpleArgParser[Prefix] =
    SimpleArgParser.from[Prefix]("prefix") { prefixStr =>
      Try(Right(Prefix(prefixStr)))
        .getOrElse(Left(Error.Other(s"Prefix [$prefixStr] is invalid")))
    }

  @CommandName("start")
  final case class StartCommand(
      @ExtraName("p")
      @HelpMessage(
        "optional argument: port on which HTTP server will be bound. " +
          "If a value is not provided, it will be randomly picked."
      )
      port: Option[Int],
      @ExtraName("o")
      @HelpMessage(
        "Config file path which has mapping of sequencers and resources needed for different observing modes. This is an Optional argument which need not be provided for simulation mode"
      )
      obsModeResourcesConfigPath: Option[Path],
      @ExtraName("l")
      @HelpMessage(
        "Option argument: true if config is to be read locally or false if from remote server: Default value: false - read config from remote server, true - read config from local in simulation mode"
      )
      local: Boolean = false,
      @HelpMessage("optional argument: agentPrefix on which sequence manager will be spawned, ex: ESW.agent1, IRIS.agent2 etc")
      @ExtraName("a")
      agentPrefix: Option[Prefix],
      @HelpMessage("simulation mode")
      simulation: Boolean = false
  ) extends SequenceManagerAppCommand
}
