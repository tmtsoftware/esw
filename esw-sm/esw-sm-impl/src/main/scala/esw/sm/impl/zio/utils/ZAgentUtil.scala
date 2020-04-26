package esw.sm.impl.zio.utils

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.api.{Failed, Spawned}
import esw.agent.client.AgentClient
import esw.commons.utils.location.EswLocationError
import esw.commons.utils.location.EswLocationError.ResolveLocationFailed
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.zio.commons.ZLocationService
import zio.{IO, Task, ZIO}
import esw.sm.api.models.AgentError
import esw.sm.api.models.SequenceManagerError.SpawnSequenceComponentFailed
import esw.sm.api.models.SequenceManagerError.LocationServiceError
import scala.util.Random

class ZAgentUtil(ZLocationService: ZLocationService)(implicit system: ActorSystem[_], timeout: Timeout) {

  private def makeAgent(agentPrefix: Prefix): Task[AgentClient] =
    ZIO.fromFuture(_ => AgentClient.make(agentPrefix, ZLocationService.locationService))

  private def getAgent: IO[EswLocationError, AgentClient] =
    ZLocationService
      .listAkkaLocationsBy(ESW, Machine)
      .flatMap(locations => makeAgent(locations.head.prefix))
      .orElseFail(ResolveLocationFailed(s"Could not find agent matching $ESW"))

  private def spawnSeqComp(agentClient: AgentClient, seqCompPrefix: Prefix): IO[AgentError, SequenceComponentApi] =
    ZIO
      .fromFuture(_ => agentClient.spawnSequenceComponent(seqCompPrefix))
      .mapError(e => SpawnSequenceComponentFailed(e.getMessage))
      .flatMap {
        case Spawned     => resolveSeqComp(seqCompPrefix)
        case Failed(msg) => ZIO.fail(SpawnSequenceComponentFailed(msg))
      }

  private def resolveSeqComp(seqCompPrefix: Prefix): IO[LocationServiceError, SequenceComponentApi] =
    ZLocationService
      .resolve(AkkaConnection(ComponentId(seqCompPrefix, SequenceComponent)))
      .bimap(e => LocationServiceError(e.msg), loc => new SequenceComponentImpl(loc))

  def spawnSequenceComponentFor(subsystem: Subsystem): IO[AgentError, SequenceComponentApi] = {
    val sequenceComponentPrefix = Prefix(subsystem, s"${subsystem}_${Random.between(1, 100)}")
    getAgent
      .mapError(e => LocationServiceError(e.msg))
      .flatMap(spawnSeqComp(_, sequenceComponentPrefix))
  }
}
