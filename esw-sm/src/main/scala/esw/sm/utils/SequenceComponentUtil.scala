package esw.sm.utils

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.models.AkkaLocation
import csw.location.api.models.ComponentType.SequenceComponent
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.ESW
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl

import scala.concurrent.Future

class SequenceComponentUtil(locationServiceUtil: LocationServiceUtil, agentUtil: AgentUtil)(
    implicit actorSystem: ActorSystem[_],
    timeout: Timeout
) {
  import actorSystem.executionContext

  def getAvailableSequenceComponent(
      subsystem: Subsystem
  ): Future[Either[SequencerError, SequenceComponentApi]] = {
    val maybeSeqCompApiF: Future[Option[SequenceComponentApi]] = subsystem match {
      case ESW => getIdleSequenceComponentFor(ESW)
      case other: Subsystem =>
        val eventualMaybeApi = getIdleSequenceComponentFor(other)
        eventualMaybeApi.flatMap {
          case Some(_) => eventualMaybeApi
          case None    => getIdleSequenceComponentFor(ESW)
        }
    }

    // spawn SeqComp if not able to find already spawned one
    maybeSeqCompApiF.flatMap {
      case Some(value) => Future.successful(Right(value))
      case None =>
        agentUtil
          .spawnSequenceComponentFor(subsystem)
          .map(Right.apply)
          .recover(e => Left(SequencerError(e.getMessage)))
    }
  }

  private def getIdleSequenceComponentFor(subsystem: Subsystem): Future[Option[SequenceComponentApi]] = {
    locationServiceUtil
      .listBy(subsystem, SequenceComponent)
      .flatMap(Future.traverse(_) { location =>
        isIdle(location).collect {
          case true => location
        }
      })
      .map(locations => locations.headOption.map(new SequenceComponentImpl(_)))
  }

  private def isIdle(sequenceComponentLocation: AkkaLocation): Future[Boolean] = {
    val sequenceComponentImpl = new SequenceComponentImpl(sequenceComponentLocation)
    sequenceComponentImpl.status.map(statusResponse => statusResponse.response.isDefined)
  }
}
