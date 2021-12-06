package esw.backend.testkit

import caseapp.RemainingArgs
import csw.prefix.models.Subsystem
import esw.backend.testkit.TSSequencerCommands.*
import esw.commons.cli.EswCommandApp
import esw.ocs.api.models.{ObsMode, Variation}
import esw.ocs.testkit.EswTestKit

object SequencerApp extends EswCommandApp[TSSequencerCommands] {
  private lazy val eswTestKit = new EswTestKit() {}

  override def run(options: TSSequencerCommands, remainingArgs: RemainingArgs): Unit =
    options match {
      case Start(subsystem: Subsystem, obsMode: ObsMode, variation: Option[Variation]) =>
        eswTestKit.spawnSequencerInSimulation(subsystem, obsMode, variation)
    }

  override def exit(code: Int): Nothing = {
    eswTestKit.shutdownAllSequencers()
    super.exit(code)
  }
}
