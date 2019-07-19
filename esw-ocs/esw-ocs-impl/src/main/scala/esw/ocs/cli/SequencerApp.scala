package esw.ocs.cli

import caseapp._
import csw.location.client.utils.LocationServerStatus
import esw.ocs.cli.SequencerAppCommand.{SequenceComponent, Sequencer}
import esw.ocs.impl.BuildInfo
import esw.ocs.internal.{SequenceComponentWiring, SequencerWiring}

object SequencerApp extends CommandApp[SequencerAppCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  def run(command: SequencerAppCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    run(command)
  }

  def run(command: SequencerAppCommand): Unit =
    command match {
      case SequenceComponent(name) => new SequenceComponentWiring(name).start()
      case Sequencer(id, mode)     => new SequencerWiring(id, mode).start()
    }
}
