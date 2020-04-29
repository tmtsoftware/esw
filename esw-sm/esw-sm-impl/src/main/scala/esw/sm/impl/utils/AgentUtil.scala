package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.api.{Failed, Spawned}
import esw.agent.client.AgentClient
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.ComponentFactory
import esw.ocs.api.SequenceComponentApi
import esw.sm.api.models.{AgentError, SequenceManagerError}

import scala.concurrent.Future
import scala.util.Random

class AgentUtil(componentFactory: ComponentFactory)(
    implicit actorSystem: ActorSystem[_]
) {

  import actorSystem.executionContext

  def spawnSequenceComponentFor(subsystem: Subsystem): Future[Either[AgentError, SequenceComponentApi]] = {
    val sequenceComponentPrefix = Prefix(subsystem, s"${subsystem}_${Random.between(1, 100)}")
    componentFactory
      .findAgent(ESW)
      .flatMap {
        case Left(error)        => futureLeft(SequenceManagerError.LocationServiceError(error.msg))
        case Right(agentClient) => spawnSeqComp(agentClient, sequenceComponentPrefix)
      }
  }

  private def spawnSeqComp(agentClient: AgentClient, seqCompPrefix: Prefix): Future[Either[AgentError, SequenceComponentApi]] =
    agentClient
      .spawnSequenceComponent(seqCompPrefix)
      .flatMap {
        case Spawned     => resolveSeqComp(seqCompPrefix)
        case Failed(msg) => futureLeft(SequenceManagerError.SpawnSequenceComponentFailed(msg))
      }

  private def resolveSeqComp(
      seqCompPrefix: Prefix
  ): Future[Either[SequenceManagerError.LocationServiceError, SequenceComponentApi]] =
    componentFactory
      .resolveSeqComp(seqCompPrefix)
      .mapLeft(e => SequenceManagerError.LocationServiceError(e.msg))

  private def futureLeft[T](v: T): Future[Left[T, Nothing]] = Future.successful(Left(v))
}
