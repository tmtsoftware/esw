package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.api.{Failed, Spawned}
import esw.agent.client.AgentClient
import esw.commons.Timeouts
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.EswLocationError.LocationNotFound
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.sm.api.protocol.AgentError
import esw.sm.api.protocol.CommonFailure.LocationServiceError

import scala.concurrent.Future
import scala.util.Random

class AgentUtil(locationServiceUtil: LocationServiceUtil)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  def spawnSequenceComponentFor(subsystem: Subsystem): Future[Either[AgentError, SequenceComponentApi]] = {
    val sequenceComponentPrefix = Prefix(subsystem, s"${subsystem}_${Random.between(1, 100)}")
    getRandomAgent(subsystem)
      .mapLeft(error => LocationServiceError(error.msg))
      .flatMapE(spawnSeqComp(_, sequenceComponentPrefix))
  }

  def spawnSequenceComponentFor(agentPrefix: Prefix, seqCompName: String): Future[Either[AgentError, SequenceComponentApi]] =
    getAgent(agentPrefix)
      .mapLeft(error => LocationServiceError(error.msg))
      .flatMapE(spawnSeqComp(_, Prefix(agentPrefix.subsystem, seqCompName)))

  private def spawnSeqComp(agentClient: AgentClient, seqCompPrefix: Prefix) =
    agentClient
      .spawnSequenceComponent(seqCompPrefix)
      .flatMap {
        case Spawned     => resolveSeqComp(seqCompPrefix)
        case Failed(msg) => Future.successful(Left(AgentError.SpawnSequenceComponentFailed(msg)))
      }

  private[utils] def getRandomAgent(subsystem: Subsystem): Future[Either[EswLocationError, AgentClient]] =
    locationServiceUtil
      .listAkkaLocationsBy(subsystem, Machine)
      // find subsystem agent randomly from list of subsystem agents (machines).
      // If this subsystem machine fails to spawn sequence component, in retry attempt randomly picking subsystem agent would help.
      // if locations are empty then locations(Random.nextInt(locations.length)).prefix will throw exception,
      // it is handled in mapError block
      .flatMapRight(locations => makeAgent(locations(Random.nextInt(locations.length)).prefix))
      .mapError(_ => LocationNotFound(s"Could not find agent matching $subsystem"))

  private[utils] def getAgent(prefix: Prefix): Future[Either[EswLocationError, AgentClient]] =
    locationServiceUtil
      .find(AkkaConnection(ComponentId(prefix, Machine)))
      .flatMapRight(location => makeAgent(location.prefix))

  private[utils] def makeAgent(prefix: Prefix): Future[AgentClient] =
    AgentClient.make(prefix, locationServiceUtil.locationService)

  private def resolveSeqComp(seqCompPrefix: Prefix) =
    locationServiceUtil
      .resolve(AkkaConnection(ComponentId(seqCompPrefix, SequenceComponent)), within = Timeouts.DefaultResolveLocationDuration)
      .mapRight(loc => new SequenceComponentImpl(loc))
      .mapLeft(e => LocationServiceError(e.msg))
}
