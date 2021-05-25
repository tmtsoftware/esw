package esw.services.cli

import java.nio.file.Path
import caseapp.{CommandName, ExtraName, HelpMessage}
import csw.prefix.models.Prefix

sealed trait Command

object Command {

  @CommandName("start")
  @HelpMessage("starts all the ESW services by default if no other option is provided")
  final case class Start(
      @ExtraName("a")
      @HelpMessage("Start agent with default ESW.primary prefix.")
      agent: Boolean = false,
      @HelpMessage("Start agent service.")
      agentService: Boolean = false,
      @HelpMessage("Prefix for agent. If provided, this will be used instead of default.")
      agentPrefix: Option[Prefix],
      @HelpMessage("Path of hostConfig file.")
      hostConfigPath: Option[String],
      @ExtraName("g")
      @HelpMessage("Start gateway with default command role config.")
      gateway: Boolean = false,
      @HelpMessage("Command role mapping file path for gateway. If provided, this will be used instead of default.")
      commandRoleConfig: Option[Path],
      @ExtraName("s")
      @HelpMessage("Start Sequence Manager with default obsMode config.")
      sequenceManager: Boolean = false,
      @HelpMessage("ObsMode config file path for Sequence Manager. If provided, this will be used instead of default.")
      obsModeConfig: Option[Path],
      @HelpMessage("Enable simulation mode for Sequence Manager")
      simulation: Boolean = false
  ) extends Command

  object Start {
    def apply(
        agent: Boolean = false,
        agentService: Boolean = false,
        agentPrefix: Option[Prefix] = None,
        hostConfigPath: Option[String] = None,
        gateway: Boolean = false,
        commandRoleConfig: Option[Path] = None,
        sequenceManager: Boolean = false,
        obsModeConfig: Option[Path] = None,
        simulation: Boolean = false
    ): Start = {
      if (agent || agentService || gateway || sequenceManager)
        new Start(
          agent,
          agentService,
          agentPrefix,
          hostConfigPath,
          gateway,
          commandRoleConfig,
          sequenceManager,
          obsModeConfig,
          simulation
        )
      else
        new Start(
          agent = true,
          agentService = true,
          agentPrefix,
          hostConfigPath,
          gateway = true,
          commandRoleConfig,
          sequenceManager = true,
          obsModeConfig,
          simulation
        )
    }
  }

  @CommandName("start-eng-ui-services")
  @HelpMessage("starts ESW services needed by ocs eng ui.\n")
  final case class StartEngUIServices(
      @HelpMessage("Enable simulation mode for sequence manager")
      smSimulationMode: Boolean = false,
      @HelpMessage("Specify sequencer-scripts version")
      scriptsVersion: Option[String] = None,
      @HelpMessage("Specify esw version")
      eswVersion: Option[String] = None
  ) extends Command
}
