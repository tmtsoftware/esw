package esw.sm.core

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.location.api.models.ComponentType.{SequenceComponent, Sequencer}
import csw.location.api.models.Connection.HttpConnection
import csw.prefix.models.{Prefix, Subsystem}
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.protocol.ScriptResponse
import esw.ocs.impl.internal.LocationServiceUtil
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.{SequenceComponentImpl, SequencerApiFactory}
import esw.sm.messages.{ConfigureResponse, SequenceManagerMsg}
import esw.sm.models.ObservingMode

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class SequenceManagerBehavior(locationService: LocationServiceUtil)(implicit val actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext
  implicit val timeout: Timeout = 10.seconds

  def beh(): Behavior[SequenceManagerMsg] = Behaviors.receiveMessage { msg =>
    def configure(obsMode: ObservingMode, replyTo: ActorRef[ConfigureResponse]) = {
      val configuredOcsSeqsF = locationService.listBy(ESW, Sequencer) // filter master Seqs from all OCS Seqs

      configuredOcsSeqsF.map { x =>
        val mayBeOcsMaster = x.find(getObsMode(_) == obsMode)

        mayBeOcsMaster match {
          case Some(value) => ocsAlreadyExists(value)
          case None        => checkResources(obsMode, replyTo)
        }
      }

      def ocsAlreadyExists(akkaLocation: AkkaLocation): Unit =
        SequencerApiFactory.make(akkaLocation).isAvailable.map {
          case true  => replyTo ! ConfigureResponse.Success(akkaLocation.prefix)
          case false => replyTo ! ConfigureResponse.AlreadyRunningObservingMode
        }

      def checkResources(obsMode: ObservingMode, replyTo: ActorRef[ConfigureResponse]): Future[Boolean] = {
        val requiredResources = extractResources(obsMode)
        val configuredResourcesF: Future[List[Resources]] =
          configuredOcsSeqsF.map(l => l.map(x => extractResources(getObsMode(x))))

        val isResourceConflictF = configuredResourcesF.map(r => r.exists(_.isConflicting(requiredResources)))

        isResourceConflictF.map {
          case true  => replyTo ! ConfigureResponse.ConflictingResourcesWithRunningObsMode
          case false => startSequencers(obsMode)
        }
        // check for conflicts
      }

      def startSequencers(observingMode: ObservingMode, replyTo: ActorRef[ConfigureResponse]): Future[List[ScriptResponse]] = {
        val requiredSequencers: Sequencers = ???
        val value =
          requiredSequencers.subsystems.map(s => getAvailableSequenceComponent(s).flatMap(_.loadScript(s, observingMode.mode)))
        val failedSequencerResponsesF = Future.sequence(value).map(_.filter(_.response.isLeft))
        failedSequencerResponsesF.map {
          case Nil =>
            locationService.locationService
              .resolve(HttpConnection(ComponentId(Prefix(ESW, observingMode.mode), Sequencer)), 5.seconds)
              .map {
                case Some(value) => replyTo.tell(ConfigureResponse.Success(value))
                case None        => replyTo ! ConfigureResponse.ConfigurationFailure("Could not find ESW Master in location service")
              }
          case failedScriptResponses: List[ScriptResponse] =>
            replyTo ! ConfigureResponse.ConfigurationFailure(failedScriptResponses.map(_.response.left))
        }
      }

      def getAvailableSequenceComponent(subsystem: Subsystem): Future[SequenceComponentApi] = {
//        locationService
//          .listBy(subsystem, SequenceComponent)
//          .map(locations =>
//            locations.flatMap(location => {
//              val impl = new SequenceComponentImpl(location.uri.toActorRef.unsafeUpcast[SequenceComponentMsg])
//              impl.status.map(_.response.isEmpty)
//
//            }))
        ???
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
