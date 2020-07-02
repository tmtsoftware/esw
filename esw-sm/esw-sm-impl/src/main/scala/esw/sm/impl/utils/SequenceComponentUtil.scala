package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.FutureUtils
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.api.protocol.SequenceComponentResponse
import esw.ocs.api.protocol.SequenceComponentResponse.{OkOrUnhandled, ScriptResponseOrUnhandled}
import esw.sm.api.protocol.AgentError.SpawnSequenceComponentFailed
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ShutdownSequenceComponentResponse.ShutdownSequenceComponentFailure
import esw.sm.api.protocol.{AgentError, ShutdownSequenceComponentResponse, SpawnSequenceComponentResponse}

import scala.async.Async._
import scala.concurrent.Future

class SequenceComponentUtil(locationServiceUtil: LocationServiceUtil, agentUtil: AgentUtil)(implicit
    actorSystem: ActorSystem[_]
) {
  import actorSystem.executionContext

  def spawnSequenceComponent(componentId: ComponentId, name: String): Future[SpawnSequenceComponentResponse] = {
    val seqCompSubsystem = componentId.prefix.subsystem
    val seqCompPrefix    = Prefix(seqCompSubsystem, name)
    agentUtil
      .spawnSequenceComponentFor(seqCompSubsystem, seqCompPrefix)
      .mapToAdt(
        _ => SpawnSequenceComponentResponse.Success(ComponentId(seqCompPrefix, SequenceComponent)),
        error => SpawnSequenceComponentFailed(error.msg)
      )
  }

  def getAvailableSequenceComponent(subsystem: Subsystem): Future[Either[AgentError, SequenceComponentApi]] =
    getIdleSequenceComponentFor(subsystem)
      .flatMap {
        case api @ Some(_)            => Future.successful(api)
        case None if subsystem != ESW => getIdleSequenceComponentFor(ESW) // fallback
        case None                     => Future.successful(None)
      }
      .flatMap {
        case Some(sequenceComponentApi) => Future.successful(Right(sequenceComponentApi))
        // spawn ESW SeqComp on ESW Machine if not able to find available sequence component of subsystem or ESW
        case None => agentUtil.spawnSequenceComponentFor(ESW)
      }

  def unloadScript(loc: AkkaLocation): Future[OkOrUnhandled] = createSequenceComponentImpl(loc).unloadScript()

  def shutdown(prefix: Prefix): Future[ShutdownSequenceComponentResponse] =
    locationServiceUtil
      .find(AkkaConnection(ComponentId(prefix, SequenceComponent)))
      .flatMapRight(createSequenceComponentImpl(_).shutdown())
      .mapToAdt(
        okOrUnhandledToShutdownSeqCompResponse(prefix),
        error => LocationServiceError(error.msg)
      )

  private def okOrUnhandledToShutdownSeqCompResponse(prefix: Prefix): OkOrUnhandled => ShutdownSequenceComponentResponse = {
    case SequenceComponentResponse.Ok                   => ShutdownSequenceComponentResponse.Success
    case SequenceComponentResponse.Unhandled(_, _, msg) => ShutdownSequenceComponentFailure(prefix, msg)
  }

  def restart(loc: AkkaLocation): Future[ScriptResponseOrUnhandled] = createSequenceComponentImpl(loc).restartScript()

  private def getIdleSequenceComponentFor(subsystem: Subsystem): Future[Option[SequenceComponentApi]] =
    locationServiceUtil
      .listAkkaLocationsBy(subsystem, SequenceComponent)
      .flatMapToAdt(raceForIdleSequenceComponents, _ => None)
  // intentionally ignoring Left as in this case domain won't decide action based on what is error hence converting it to optionality

  private def raceForIdleSequenceComponents(locations: List[AkkaLocation]) =
    FutureUtils
      .firstCompletedOf(locations.map(idleSequenceComponent))(_.isDefined)
      .map(_.flatten)

  private[sm] def idleSequenceComponent(sequenceComponentLocation: AkkaLocation): Future[Option[SequenceComponentApi]] =
    async {
      val sequenceComponentApi = createSequenceComponentImpl(sequenceComponentLocation)
      val isAvailable          = await(sequenceComponentApi.status).response.isEmpty
      if (isAvailable) Some(sequenceComponentApi) else None
    }

  private[sm] def createSequenceComponentImpl(sequenceComponentLocation: AkkaLocation) =
    new SequenceComponentImpl(sequenceComponentLocation)
}
