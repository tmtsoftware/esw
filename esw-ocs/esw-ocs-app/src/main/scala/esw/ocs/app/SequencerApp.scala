package esw.ocs.app

import caseapp.{CommandApp, RemainingArgs}
import csw.location.client.utils.LocationServerStatus
import esw.ocs.app.SequencerAppCommand.{SequenceComponent, Sequencer}
import esw.ocs.internal.{ActorRuntime, SequenceComponentWiring, SequencerWiring}

import scala.util.control.NonFatal

object SequencerApp extends CommandApp[SequencerAppCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  def run(command: SequencerAppCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    run(command)
  }

  def run(command: SequencerAppCommand, enableLogging: Boolean = true): Unit = {
    command match {
      case SequenceComponent(name) =>
        val wiring = new SequenceComponentWiring(name)
        import wiring.actorRuntime._

        withTry(name, wiring.actorRuntime) {
          val registrationResult = wiring.start()
          log.info(
            s"Successfully started and registered Sequence Component with name: [$name] and RegistrationResult: [$registrationResult]"
          )
        }

      case Sequencer(id, mode) =>
        val wiring = new SequencerWiring(id, mode)
        import wiring.actorRuntime._

        withTry(wiring.name, wiring.actorRuntime) {
          wiring.start()
          log.info(s"Successfully started Sequencer with name : ${wiring.name}")
        }
    }

    def withTry(name: String, actorRuntime: ActorRuntime)(thunk: => Unit): Unit = {
      import actorRuntime._
      try {
        if (enableLogging) startLogging(name)
        thunk
      } catch {
        case NonFatal(e) =>
          log.error(s"Failed to start $name", ex = e)
          typedSystem.terminate()
          throw e
      }
    }
  }

}
