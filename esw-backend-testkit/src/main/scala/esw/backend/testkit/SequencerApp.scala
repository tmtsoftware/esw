package esw.backend.testkit

import caseapp.{Command, RemainingArgs}
import caseapp.core.app.{Command, CommandsEntryPoint}
import caseapp.core.help.Help
import caseapp.core.parser.Parser
import csw.prefix.models.Subsystem
import esw.agent.pekko.app.AgentCliCommand
import esw.backend.testkit.TSSequencerCommands.*
import esw.commons.cli.EswCommand
import esw.ocs.api.models.{ObsMode, Variation}
import esw.ocs.testkit.EswTestKit

object SequencerApp extends CommandsEntryPoint {

  override def commands: Seq[Command[?]] = Seq(StartCommand)

  override def progName: String = getClass.getSimpleName.dropRight(1) // remove $ from class name

  val StartCommand: Runner[StartOptions] = Runner[StartOptions]()

  class Runner[T <: TSSequencerCommands: Parser: Help] extends EswCommand[T] {
    override def run(command: T, args: RemainingArgs): Unit = {
      command match {
        case StartOptions(subsystem: Subsystem, obsMode: ObsMode, variation: Option[Variation]) =>
          eswTestKit.spawnSequencerInSimulation(subsystem, obsMode, variation)
      }
    }

    private lazy val eswTestKit = new EswTestKit() {}

    override def exit(code: Int): Nothing = {
      eswTestKit.shutdownAllSequencers()
      super.exit(code)
    }
  }

}
