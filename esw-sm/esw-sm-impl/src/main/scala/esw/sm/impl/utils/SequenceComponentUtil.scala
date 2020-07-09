package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.FutureUtils
import esw.commons.utils.location.EswLocationError.RegistrationListingFailed
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.api.protocol.SequenceComponentResponse
import esw.ocs.api.protocol.SequenceComponentResponse.{Ok, ScriptResponseOrUnhandled}
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ShutdownSequenceComponentsPolicy.{AllSequenceComponents, SingleSequenceComponent}
import esw.sm.api.protocol.{
  AgentError,
  ShutdownSequenceComponentResponse,
  ShutdownSequenceComponentsPolicy,
  SpawnSequenceComponentResponse
}

import scala.async.Async._
import scala.concurrent.Future

class SequenceComponentUtil(locationServiceUtil: LocationServiceUtil, agentUtil: AgentUtil)(implicit
    actorSystem: ActorSystem[_]
) {
  import actorSystem.executionContext

  def spawnSequenceComponent(machine: Prefix, name: String): Future[SpawnSequenceComponentResponse] = {
    val seqCompPrefix = Prefix(machine.subsystem, name)
    agentUtil
      .spawnSequenceComponentFor(machine, name)
      .mapToAdt(
        _ => SpawnSequenceComponentResponse.Success(ComponentId(seqCompPrefix, SequenceComponent)),
        identity
      )
  }

  def idleSequenceComponentsFor(
      subsystems: List[Subsystem]
  ): Future[Either[RegistrationListingFailed, List[AkkaLocation]]] =
    locationServiceUtil
      .listAkkaLocationsBy(SequenceComponent, withFilter = location => subsystems.contains(location.prefix.subsystem))
      .flatMapRight(filterIdleSequenceComponents)

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

  def unloadScript(loc: AkkaLocation): Future[Ok.type] = createSequenceComponentImpl(loc).unloadScript()

  def shutdown(policy: ShutdownSequenceComponentsPolicy): Future[ShutdownSequenceComponentResponse] =
    (policy match {
      case SingleSequenceComponent(prefix) => shutdown(prefix)
      case AllSequenceComponents           => shutdownAll().mapRight(_ => SequenceComponentResponse.Ok)
    }).mapToAdt(_ => ShutdownSequenceComponentResponse.Success, error => LocationServiceError(error.msg))

  def restartScript(loc: AkkaLocation): Future[ScriptResponseOrUnhandled] = createSequenceComponentImpl(loc).restartScript()

  private def shutdown(prefix: Prefix): Future[Either[EswLocationError.FindLocationError, SequenceComponentResponse.Ok.type]] =
    locationServiceUtil
      .find(AkkaConnection(ComponentId(prefix, SequenceComponent)))
      .flatMapRight(shutdown)

  private def shutdownAll(): Future[Either[EswLocationError.RegistrationListingFailed, List[SequenceComponentResponse.Ok.type]]] =
    locationServiceUtil
      .listAkkaLocationsBy(SequenceComponent)
      .flatMapRight(Future.traverse(_)(shutdown))

  private def shutdown(loc: AkkaLocation): Future[SequenceComponentResponse.Ok.type] = createSequenceComponentImpl(loc).shutdown()

  private def getIdleSequenceComponentFor(subsystem: Subsystem): Future[Option[SequenceComponentApi]] =
    locationServiceUtil
      .listAkkaLocationsBy(subsystem, SequenceComponent)
      .flatMapToAdt(raceForIdleSequenceComponents, _ => None)
  // intentionally ignoring Left as in this case domain won't decide action based on what is error hence converting it to optionality

  private def raceForIdleSequenceComponents(locations: List[AkkaLocation]) =
    FutureUtils
      .firstCompletedOf(locations.map(idleSequenceComponent))(_.isDefined)
      .map(_.flatten)

  //todo: to be removed
  private[sm] def idleSequenceComponent(sequenceComponentLocation: AkkaLocation): Future[Option[SequenceComponentApi]] =
    async {
      val sequenceComponentApi = createSequenceComponentImpl(sequenceComponentLocation)
      val isAvailable          = await(sequenceComponentApi.status).response.isEmpty
      if (isAvailable) Some(sequenceComponentApi) else None
    }

  private def filterIdleSequenceComponents(locations: List[AkkaLocation]): Future[List[AkkaLocation]] = {
    Future
      .traverse(locations)(idleSeqComp)
      .map(_.collect { case Some(location) => location })
  }

  private[sm] def idleSeqComp(sequenceComponentLocation: AkkaLocation): Future[Option[AkkaLocation]] =
    async {
      val sequenceComponentApi = createSequenceComponentImpl(sequenceComponentLocation)
      val isIdle               = await(sequenceComponentApi.status).response.isEmpty
      if (isIdle) Some(sequenceComponentLocation) else None
    }

  private[sm] def createSequenceComponentImpl(sequenceComponentLocation: AkkaLocation): SequenceComponentApi =
    new SequenceComponentImpl(sequenceComponentLocation)
}
