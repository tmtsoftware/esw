package esw.sm.core

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.location.api.models.AkkaLocation
import csw.location.api.models.ComponentType.Sequencer
import csw.prefix.models.Subsystem.ESW
import esw.ocs.impl.SequencerApiFactory
import esw.ocs.impl.internal.LocationServiceUtil
import esw.sm.messages.{ConfigureResponse, SequenceManagerMsg}
import esw.sm.models.ObservingMode

import scala.concurrent.Future

class SequenceManagerBehavior(locationService: LocationServiceUtil) {

  def beh(): Behavior[SequenceManagerMsg] = Behaviors.receive { (ctx, msg) =>
    import ctx.executionContext
    implicit val system: ActorSystem[_] = ctx.system

    def configure(obsMode: ObservingMode, replyTo: ActorRef[ConfigureResponse]) = {
      val configuredOcsSeqsF = locationService.listBy(ESW, Sequencer) // filter master Seqs from all OCS Seqs

      configuredOcsSeqsF.map { x =>
        val mayBeOcsMaster = x.find(getObsMode(_) == obsMode)

        mayBeOcsMaster match {
          case Some(value) => ocsAlreadyExists(value)
          case None        => checkResources(obsMode)
        }
      }

      def ocsAlreadyExists(akkaLocation: AkkaLocation): Unit =
        SequencerApiFactory.make(akkaLocation).isAvailable.map {
          case true  => replyTo ! ConfigureResponse.Success(akkaLocation.prefix)
          case false => replyTo ! ConfigureResponse.AlreadyRunningObservingMode
        }

      def checkResources(obsMode: ObservingMode): Unit = {
        val requiredResources          = extractResources(obsMode)
        val a: Future[List[Resources]] = configuredOcsSeqsF.map(l => l.map(x => extractResources(getObsMode(x))))

        ???
        // check for conflicts
      }

      def getObsMode(akkaLocation: AkkaLocation): ObservingMode = ObservingMode(akkaLocation.prefix.componentName)

      def extractResources(observingMode: ObservingMode): Resources = ???
    }

    msg match {
      case SequenceManagerMsg.Cleanup(observingMode, replyTo)   => Behaviors.same
      case SequenceManagerMsg.Configure(observingMode, replyTo) => configure(observingMode, replyTo); ???;
      case SequenceManagerMsg.GetRunningObsModes(replyTo)       => Behaviors.same
      case _                                                    => Behaviors.same
    }
  }
}
