package esw.services.cli

import caseapp.core.argparser.SimpleArgParser
import caseapp.{CommandName, ExtraName, HelpMessage}
import csw.prefix.models.Prefix
import esw.sm.app.SequenceManagerAppCommand

import java.nio.file.Path

sealed trait Command

object Command {

  implicit val prefixParser: SimpleArgParser[Prefix] = SequenceManagerAppCommand.prefixParser

  @CommandName("start")
  @HelpMessage("starts all the ESW services by default if no other option is provided")
  final case class Start(
      @ExtraName("a")
      @HelpMessage("Start agent with default ESW.primary prefix.")
      agent: Boolean = false,
      @HelpMessage("Prefix for agent. If provided, this will be used instead of default.")
      agentPrefix: Option[Prefix],
      @ExtraName("g")
      @HelpMessage("Start gateway with default command role config.")
      gateway: Boolean = false,
      @HelpMessage("Command role mapping file path for gateway. If provided, this will be used instead of default.")
      commandRoleConfig: Option[Path],
      @ExtraName("s")
      @HelpMessage("Start sequence manager with default obsMode config.")
      sequenceManager: Boolean = false,
      @HelpMessage("ObsMode config file path for gateway. If provided, this will be used instead of default.")
      obsModeConfig: Option[Path]
  ) extends Command

  object Start {
    def apply(
        agent: Boolean = false,
        agentPrefix: Option[Prefix] = None,
        gateway: Boolean = false,
        commandRoleConfig: Option[Path] = None,
        sequenceManager: Boolean = false,
        obsModeConfig: Option[Path] = None
    ): Start = {
      if (agent || gateway || sequenceManager)
        new Start(agent, agentPrefix, gateway, commandRoleConfig, sequenceManager, obsModeConfig)
      else
        new Start(agent = true, agentPrefix, gateway = true, commandRoleConfig, sequenceManager = true, obsModeConfig)
    }
  }
}
