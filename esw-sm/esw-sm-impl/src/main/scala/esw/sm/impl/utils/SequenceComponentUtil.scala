package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.FutureUtils
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.SequenceComponentResponse.{Ok, ScriptResponseOrUnhandled, SequencerLocation, Unhandled}
import esw.ocs.api.protocol.{ScriptError, SequenceComponentResponse}
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ShutdownSequenceComponentsPolicy.{AllSequenceComponents, SingleSequenceComponent}
import esw.sm.api.protocol.StartSequencerResponse.{LoadScriptError, SequenceComponentNotAvailable, Started}
import esw.sm.api.protocol._
import esw.sm.impl.config.ProvisionConfig

import scala.async.Async._
import scala.concurrent.Future

class SequenceComponentUtil(locationServiceUtil: LocationServiceUtil, agentUtil: AgentUtil)(implicit
    actorSystem: ActorSystem[_]
) {
  import actorSystem.executionContext

  def spawnSequenceComponent(machine: Prefix, name: String): Future[SpawnSequenceComponentResponse] = {
    agentUtil.spawnSequenceComponentOn(machine, name)
  }

  // return mapping of subsystems for which idle sequence components are available
  def getAllIdleSequenceComponentsFor(
      subsystems: List[Subsystem]
  ): Future[Either[LocationServiceError, List[AkkaLocation]]] = {
    locationServiceUtil
      .listAkkaLocationsBy(SequenceComponent, withFilter = location => subsystems.contains(location.prefix.subsystem))
      .flatMapRight(filterIdleSequenceComponents)
      .mapLeft(error => LocationServiceError(error.msg))
  }

  def loadScript(
      subSystem: Subsystem,
      obsMode: ObsMode
  ): Future[Either[StartSequencerResponse.Failure, Started]] =
    getAvailableSequenceComponent(subSystem).flatMap {
      case Left(error)       => Future.successful(Left(error))
      case Right(seqCompLoc) => loadScript(subSystem, obsMode, seqCompLoc)
    }

  def loadScript(
      subSystem: Subsystem,
      obsMode: ObsMode,
      seqCompLoc: AkkaLocation
  ): Future[Either[StartSequencerResponse.Failure, Started]] = {
    val seqCompApi = createSequenceComponentImpl(seqCompLoc)
    seqCompApi
      .loadScript(subSystem, obsMode)
      .flatMap {
        case SequencerLocation(location)             => Future.successful(Right(Started(location.connection.componentId)))
        case error: ScriptError.LocationServiceError => Future.successful(Left(LocationServiceError(error.msg)))
        case error: ScriptError.LoadingScriptFailed  => Future.successful(Left(LoadScriptError(error.msg)))
        case error: Unhandled                        => Future.successful(Left(LoadScriptError(error.msg)))
      }
  }

  def unloadScript(loc: AkkaLocation): Future[Ok.type] = createSequenceComponentImpl(loc).unloadScript()

  def shutdown(policy: ShutdownSequenceComponentsPolicy): Future[ShutdownSequenceComponentResponse] =
    (policy match {
      case SingleSequenceComponent(prefix) => shutdown(prefix)
      case AllSequenceComponents           => shutdownAll().mapRight(_ => SequenceComponentResponse.Ok)
    }).mapToAdt(_ => ShutdownSequenceComponentResponse.Success, error => LocationServiceError(error.msg))

  def restartScript(loc: AkkaLocation): Future[ScriptResponseOrUnhandled] = createSequenceComponentImpl(loc).restartScript()

  def provision(config: ProvisionConfig): Future[ProvisionResponse] = agentUtil.provision(config)

  def getSequenceComponentStatus(seqCompIds: List[ComponentId]): Future[Map[ComponentId, Option[AkkaLocation]]] = {
    Future
      .traverse(seqCompIds) { seqComp =>
        locationServiceUtil.find(AkkaConnection(seqComp)).flatMap {
          case Left(locationError)    => Future.successful(List.empty)
          case Right(seqCompLocation) => createSequenceComponentImpl(seqCompLocation).status.map(s => List(seqComp -> s.response))
        }
      }
      .map(_.flatten.toMap)
  }

  private def getAvailableSequenceComponent(subsystem: Subsystem): Future[Either[SequenceComponentNotAvailable, AkkaLocation]] =
    getIdleSequenceComponentFor(subsystem)
      .flatMap {
        case location @ Some(_)       => Future.successful(location)
        case None if subsystem != ESW => getIdleSequenceComponentFor(ESW) // fallback
        case None                     => Future.successful(None)
      }
      .map {
        case Some(location) => Right(location)
        case None           => Left(SequenceComponentNotAvailable(subsystem))
      }

  private def shutdown(prefix: Prefix): Future[Either[EswLocationError.FindLocationError, SequenceComponentResponse.Ok.type]] =
    locationServiceUtil
      .find(AkkaConnection(ComponentId(prefix, SequenceComponent)))
      .flatMapRight(shutdown)

  private def shutdownAll(): Future[Either[EswLocationError.RegistrationListingFailed, List[SequenceComponentResponse.Ok.type]]] =
    locationServiceUtil
      .listAkkaLocationsBy(SequenceComponent)
      .flatMapRight(Future.traverse(_)(shutdown))

  private def shutdown(loc: AkkaLocation): Future[SequenceComponentResponse.Ok.type] = createSequenceComponentImpl(loc).shutdown()

  private def getIdleSequenceComponentFor(subsystem: Subsystem): Future[Option[AkkaLocation]] =
    locationServiceUtil
      .listAkkaLocationsBy(subsystem, SequenceComponent)
      .flatMapToAdt(raceForIdleSequenceComponents, _ => None)
  // intentionally ignoring Left as in this case domain won't decide action based on what is error hence converting it to optionality

  private def raceForIdleSequenceComponents(locations: List[AkkaLocation]): Future[Option[AkkaLocation]] =
    FutureUtils
      .firstCompletedOf(locations.map(idleSequenceComponent))(_.isDefined)
      .map(_.flatten)

  private def filterIdleSequenceComponents(locations: List[AkkaLocation]): Future[List[AkkaLocation]] = {
    Future
      .traverse(locations)(idleSequenceComponent)
      .map(_.collect { case Some(location) => location })
  }

  private[sm] def idleSequenceComponent(sequenceComponentLocation: AkkaLocation): Future[Option[AkkaLocation]] =
    async {
      val sequenceComponentApi = createSequenceComponentImpl(sequenceComponentLocation)
      val isIdle               = await(sequenceComponentApi.status).response.isEmpty
      if (isIdle) Some(sequenceComponentLocation) else None
    }

  private[sm] def createSequenceComponentImpl(sequenceComponentLocation: AkkaLocation): SequenceComponentApi =
    new SequenceComponentImpl(sequenceComponentLocation)
}
