package esw.sm.utils

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.api.{Failed, Spawned}
import esw.agent.client.AgentClient
import esw.commons.utils.FutureEitherUtils.FutureEither
import esw.commons.utils.location.EswLocationError.ResolveLocationFailed
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl

import scala.concurrent.Future
import scala.util.Random
import scala.util.control.NonFatal

class AgentUtil(locationServiceUtil: LocationServiceUtil)(implicit actorSystem: ActorSystem[_], timeout: Timeout) {

  import actorSystem.executionContext

  private[sm] def getAgent: Future[Either[EswLocationError, AgentClient]] =
    locationServiceUtil
      .listBy(ESW, Machine)
      .flatRight(locations => AgentClient.make(locations.head.prefix, locationServiceUtil.locationService).map(Right(_)))
      .recover {
        // This covers case where AgentClient make can throw RuntimeException
        case NonFatal(_) => Left(ResolveLocationFailed(s"Could not find agent matching $ESW"))
      }

  def spawnSequenceComponentFor(subsystem: Subsystem): Future[Either[AgentError, SequenceComponentApi]] = {
    val sequenceComponentPrefix = Prefix(subsystem, s"${subsystem}_${Random.between(1, 100)}")

    getAgent
      .flatMap {
        case Left(error) => Future.successful(Left(SequenceManagerError.LocationServiceError(error.msg)))
        case Right(agentClient) =>
          agentClient
            .spawnSequenceComponent(sequenceComponentPrefix)
            .flatMap {
              case Spawned =>
                locationServiceUtil
                  .resolveAkkaLocation(sequenceComponentPrefix, SequenceComponent)
                  .map {
                    case Left(error)     => Left(SequenceManagerError.LocationServiceError(error.msg))
                    case Right(location) => Right(new SequenceComponentImpl(location))
                  }
              case Failed(msg) => Future.successful(Left(SequenceManagerError.SpawnSequenceComponentFailed(msg)))
            }
      }
  }
}
