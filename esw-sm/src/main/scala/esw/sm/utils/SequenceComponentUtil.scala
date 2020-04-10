package esw.sm.utils

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
import csw.location.api.models.ComponentType.SequenceComponent
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.SequenceComponentApi
import esw.ocs.impl.SequenceComponentImpl
import esw.ocs.impl.internal.LocationServiceUtil
import esw.ocs.impl.messages.SequenceComponentMsg

import scala.concurrent.Future

class SequenceComponentUtil(locationService: LocationServiceUtil)(implicit actorSystem: ActorSystem[_], timeout: Timeout) {
  import actorSystem.executionContext

  def getAvailableSequenceComponent(subsystem: Subsystem): Future[SequenceComponentApi] = {
    val maybeSeqCompApiF: Future[Option[SequenceComponentApi]] = subsystem match {
      case ESW => getIdleSequenceComponentFor(ESW)
      case other: Subsystem =>
        val eventualMaybeApi = getIdleSequenceComponentFor(other)
        eventualMaybeApi.flatMap {
          case Some(_) => eventualMaybeApi
          case None    => getIdleSequenceComponentFor(ESW)
        }

    }
    maybeSeqCompApiF.map {
      case Some(seqCompApi) => seqCompApi
      case None             => spwanSequenceComponentFor(subsystem)
    }
  }

  def getIdleSequenceComponentFor(subsystem: Subsystem): Future[Option[SequenceComponentApi]] = {
    locationService
      .listBy(subsystem, SequenceComponent)
      .flatMap(locations => getIdleSequenceComponentFrom(locations))
      .map {
        case Some(value) =>
          val sequenceComponentApi: SequenceComponentApi =
            new SequenceComponentImpl(value.uri.toActorRef.unsafeUpcast[SequenceComponentMsg])
          Some(sequenceComponentApi)
        case None => None
      }
  }

  def getIdleSequenceComponentFrom(locations: List[AkkaLocation]): Future[Option[AkkaLocation]] = {
    locations
      .map(location => {
        isIdle(location).map {
          case true  => Some(location)
          case false => None
        }
      })
      .head
  }

  def isIdle(sequenceComponentLocation: AkkaLocation): Future[Boolean] = {
    val sequenceComponentImpl = new SequenceComponentImpl(
      sequenceComponentLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg])
    sequenceComponentImpl.status.map(statusResponse => statusResponse.response.isDefined)
  }
}
