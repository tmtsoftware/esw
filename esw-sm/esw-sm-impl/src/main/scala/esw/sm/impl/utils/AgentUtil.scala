package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.api.{Failed, Spawned}
import esw.agent.client.AgentClient
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.EswLocationError.ResolveLocationFailed
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.sm.api.models.{AgentError, SequenceManagerError}

import scala.concurrent.Future
import scala.util.Random

class AgentUtil(locationServiceUtil: LocationServiceUtil)(implicit actorSystem: ActorSystem[_], timeout: Timeout) {

  import actorSystem.executionContext

  private[sm] def getAgent: Future[Either[EswLocationError, AgentClient]] =
    locationServiceUtil
      .listAkkaLocationsBy(ESW, Machine)
      // if locations.head.prefix throws exception, it is handled in mapError block
      .flatMapRight(locations => AgentClient.make(locations.head.prefix, locationServiceUtil.locationService))
      .mapError(_ => ResolveLocationFailed(s"Could not find agent matching $ESW"))

  def spawnSequenceComponentFor(subsystem: Subsystem): Future[Either[AgentError, SequenceComponentApi]] = {
    val sequenceComponentPrefix =
      Prefix(subsystem, s"${subsystem}_${Random.between(1, 100)}")
    getAgent
      .flatMap {
        case Left(error) =>
          futureLeft(SequenceManagerError.LocationServiceError(error.msg))
        case Right(agentClient) =>
          spawnSeqComp(agentClient, sequenceComponentPrefix)
      }
  }

  private def spawnSeqComp(agentClient: AgentClient, seqCompPrefix: Prefix) =
    agentClient
      .spawnSequenceComponent(seqCompPrefix)
      .flatMap {
        case Spawned => resolveSeqComp(seqCompPrefix)
        case Failed(msg) =>
          futureLeft(SequenceManagerError.SpawnSequenceComponentFailed(msg))
      }

  private def resolveSeqComp(seqCompPrefix: Prefix) =
    locationServiceUtil
      .resolve(AkkaConnection(ComponentId(seqCompPrefix, SequenceComponent)))
      .mapRight(loc => new SequenceComponentImpl(loc))
      .mapLeft(e => SequenceManagerError.LocationServiceError(e.msg))

  private def futureLeft[T](v: T) = Future.successful(Left(v))
}
