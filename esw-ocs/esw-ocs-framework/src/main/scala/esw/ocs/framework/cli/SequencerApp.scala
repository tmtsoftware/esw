package esw.ocs.framework.cli

import caseapp._
import esw.ocs.framework.BuildInfo
import esw.ocs.framework.cli.SequencerAppCommand.{SequenceComponent, Sequencer}
import esw.ocs.framework.internal.{SequenceComponentWiring, SequencerWiring}

object SequencerApp extends CommandApp[SequencerAppCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  def run(command: SequencerAppCommand, args: RemainingArgs): Unit =
    command match {
      case SequenceComponent(name) => new SequenceComponentWiring(name).start()
      case Sequencer(id, mode)     => new SequencerWiring(id, mode).start()
    }
}
