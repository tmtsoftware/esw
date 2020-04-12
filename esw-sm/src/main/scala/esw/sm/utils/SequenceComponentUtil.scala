package esw.sm.utils

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.models.AkkaLocation
import csw.location.api.models.ComponentType.SequenceComponent
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.SequenceComponentApi
import esw.ocs.impl.SequenceComponentImpl
import esw.ocs.impl.internal.LocationServiceUtil
import esw.sm.utils.RichAkkaLocation._

import scala.async.Async._
import scala.concurrent.Future
import scala.util.control.NonFatal

case class Error(msg: String)

class SequenceComponentUtil(locationServiceUtil: LocationServiceUtil, agentUtil: AgentUtil)(implicit actorSystem: ActorSystem[_],
                                                                                            timeout: Timeout) {
  import actorSystem.executionContext

  def getAvailableSequenceComponent(subsystem: Subsystem): Future[Either[Error, SequenceComponentApi]] = {
    val maybeSeqCompApiF: Future[Option[SequenceComponentApi]] = subsystem match {
      case ESW => getIdleSequenceComponentFor(ESW)
      case other: Subsystem =>
        val eventualMaybeApi = getIdleSequenceComponentFor(other)
        eventualMaybeApi.flatMap {
          case Some(_) => eventualMaybeApi
          case None    => getIdleSequenceComponentFor(ESW)
        }

    }
    maybeSeqCompApiF.flatMap {
      case Some(value) => Future.successful(Right(value))
      case None =>
        try {
          async(
            Right(await(agentUtil.spawnSequenceComponentFor(subsystem)))
          )
        } catch {
          case NonFatal(e) => Future.successful(Left(Error(e.getMessage)))
        }
    }
  }

  private def getIdleSequenceComponentFor(subsystem: Subsystem): Future[Option[SequenceComponentApi]] = {
    for {
      seqCompLocations          <- locationServiceUtil.listBy(subsystem, SequenceComponent)
      availableSeqCompLocations <- getAvailableSequenceComponentFrom(seqCompLocations)
      maybeSeqCompLocation      = availableSeqCompLocations.headOption
    } yield {
      maybeSeqCompLocation.map(seqCompLocation => new SequenceComponentImpl(seqCompLocation.toSequenceComponentRef))
    }
  }

  private def getAvailableSequenceComponentFrom(locations: List[AkkaLocation]): Future[List[AkkaLocation]] =
    async(locations.filter(location => await(isIdle(location))))

  private def isIdle(sequenceComponentLocation: AkkaLocation): Future[Boolean] = {
    val sequenceComponentImpl = new SequenceComponentImpl(sequenceComponentLocation.toSequenceComponentRef)
    sequenceComponentImpl.status.map(statusResponse => statusResponse.response.isDefined)
  }
}
