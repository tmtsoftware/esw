package esw.sm.utils

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.api.{Failed, Spawned}
import esw.agent.client.AgentClient
import esw.ocs.api.SequenceComponentApi
import esw.ocs.impl.SequenceComponentImpl
import esw.ocs.impl.internal.LocationServiceUtil
import esw.ocs.impl.messages.SequenceComponentMsg

import scala.async.Async._
import scala.concurrent.Future
import scala.util.Random

class SequenceComponentUtil(locationService: LocationServiceUtil)(implicit actorSystem: ActorSystem[_], timeout: Timeout) {
  import actorSystem.executionContext

  def getAvailableSequenceComponent(subsystem: Subsystem): Future[Option[SequenceComponentApi]] = {
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
      case Some(seqCompApi) => Future.successful(Some(seqCompApi))
      case None             => spawnSequenceComponentFor(subsystem)
    }
  }

  def getAgent: Future[AgentClient] = {
    locationService
      .listBy(ESW, Machine)
      .flatMap(locations => AgentClient.make(locations.head.prefix, locationService.locationService))
  }

  def spawnSequenceComponentFor(subsystem: Subsystem): Future[Option[SequenceComponentApi]] = {
    val sequenceComponentPrefix = Prefix(subsystem, s"${subsystem}_${Random.between(1, 100)}")
    for {
      agentClient   <- getAgent
      spawnResponse <- agentClient.spawnSequenceComponent(sequenceComponentPrefix)
    } yield {
      await(spawnResponse match {
        case Spawned =>
          locationService
            .resolveAkkaLocation(sequenceComponentPrefix, SequenceComponent)
            .map(location => {
              val seqCompRef = location.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]
              Some(new SequenceComponentImpl(seqCompRef))
            })
        case Failed(_) => Future.successful(None)
      })
    }
  }

  private def getIdleSequenceComponentFor(subsystem: Subsystem): Future[Option[SequenceComponentApi]] = {
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

  private def getIdleSequenceComponentFrom(locations: List[AkkaLocation]): Future[Option[AkkaLocation]] = async {
    locations
      .map(location => {
        if (await(isIdle(location))) Some(location) else None
      })
      .head
  }

  private def isIdle(sequenceComponentLocation: AkkaLocation): Future[Boolean] = {
    val sequenceComponentImpl = new SequenceComponentImpl(
      sequenceComponentLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg])
    sequenceComponentImpl.status.map(statusResponse => statusResponse.response.isDefined)
  }
}
