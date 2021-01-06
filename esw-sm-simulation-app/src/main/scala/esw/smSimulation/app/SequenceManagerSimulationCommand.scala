package esw.smSimulation.app

import java.nio.file.Path

import caseapp.core.Error
import caseapp.core.argparser.SimpleArgParser
import caseapp.{CommandName, ExtraName, HelpMessage}
import csw.prefix.models.Prefix

import scala.util.Try

sealed trait SequenceManagerSimulationCommand

object SequenceManagerSimulationCommand {

  implicit val prefixParser: SimpleArgParser[Prefix] =
    SimpleArgParser.from[Prefix]("prefix") { prefixStr =>
      Try(Right(Prefix(prefixStr)))
        .getOrElse(Left(Error.Other(s"Prefix [$prefixStr] is invalid")))
    }

  @CommandName("start")
  final case class StartCommand(
      @ExtraName("o")
      @HelpMessage(
        "Config file path which has mapping of sequencers and resources needed for different observing modes"
      )
      obsModeConfigPath: Option[Path],
      @ExtraName("l")
      @HelpMessage(
        "Option argument: true if config is to be read locally or false if from remote server: Default value: false - read config from remote server"
      )
      isLocal: Boolean = true,
      @HelpMessage("optional argument: agentPrefix on which sequence manager will be spawned, ex: ESW.agent1, IRIS.agent2 etc")
      @ExtraName("a")
      agentPrefix: Option[Prefix]
  ) extends SequenceManagerSimulationCommand
}
