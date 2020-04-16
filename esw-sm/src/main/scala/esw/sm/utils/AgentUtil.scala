package esw.sm.utils

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.api.{Failed, Spawned}
import esw.agent.client.AgentClient
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl

import scala.concurrent.Future
import scala.util.Random

class AgentUtil(locationServiceUtil: LocationServiceUtil)(implicit actorSystem: ActorSystem[_], timeout: Timeout) {
  import actorSystem.executionContext

  private[sm] def getAgent: Future[AgentClient] =
    locationServiceUtil
      .listBy(ESW, Machine)
      .flatMap(locations => AgentClient.make(locations.head.prefix, locationServiceUtil.locationService))

  def spawnSequenceComponentFor(subsystem: Subsystem): Future[Either[SequencerError, SequenceComponentApi]] = {
    val sequenceComponentPrefix = Prefix(subsystem, s"${subsystem}_${Random.between(1, 100)}")
    getAgent
      .flatMap(_.spawnSequenceComponent(sequenceComponentPrefix).flatMap {
        case Spawned =>
          locationServiceUtil
            .resolveAkkaLocation(sequenceComponentPrefix, SequenceComponent)
            .map(location => Right(new SequenceComponentImpl(location)))
        case Failed(msg) => Future.successful(Left(SequencerError(msg)))
      })
      .recover(ex => Left(SequencerError(ex.getMessage)))
  }
}
