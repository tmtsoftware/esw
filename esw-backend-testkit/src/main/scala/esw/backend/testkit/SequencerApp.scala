package esw.backend.testkit

import caseapp.RemainingArgs
import csw.prefix.models.{Prefix, Subsystem}
import esw.backend.testkit.TSSequencerCommands.*
import esw.commons.cli.EswCommandApp
import esw.ocs.testkit.EswTestKit

object SequencerApp extends EswCommandApp[TSSequencerCommands] {
  private lazy val eswTestKit = new EswTestKit() {}

  override def run(options: TSSequencerCommands, remainingArgs: RemainingArgs): Unit =
    options match {
      case Start(subsystem: Subsystem, componentName: String) =>
        eswTestKit.spawnSequencerInSimulation(Prefix(subsystem, componentName))
    }

  override def exit(code: Int): Nothing = {
    eswTestKit.shutdownAllSequencers()
    super.exit(code)
  }
}
