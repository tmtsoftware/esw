package esw.ocs.app

import caseapp.{CommandApp, RemainingArgs}
import csw.location.client.utils.LocationServerStatus
import esw.ocs.app.SequencerAppCommand.{SequenceComponent, Sequencer}
import esw.ocs.internal.{ActorRuntime, SequenceComponentWiring, SequencerWiring}

object SequencerApp extends CommandApp[SequencerAppCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  def run(command: SequencerAppCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    run(command)
  }

  def run(command: SequencerAppCommand, startLogging: Boolean = true): Unit = {
    def enableLogging(name: String, actorRuntime: ActorRuntime): Unit = if (startLogging) actorRuntime.startLogging(name)

    command match {
      case SequenceComponent(name) =>
        val wiring = new SequenceComponentWiring(name)
        import wiring.actorRuntime._
        enableLogging(name, wiring.actorRuntime)
        wiring.start()
        log.info(s"Successfully started Sequence Component with name: $name")

      case Sequencer(id, mode) =>
        val wiring = new SequencerWiring(id, mode)
        import wiring.actorRuntime._
        enableLogging(wiring.name, wiring.actorRuntime)
        wiring.start()
        log.info(s"Successfully started Sequencer with name : ${wiring.name}")
    }
  }

}
