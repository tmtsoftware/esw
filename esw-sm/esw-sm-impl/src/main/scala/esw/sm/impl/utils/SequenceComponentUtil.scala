package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.SequenceComponentResponse.{Ok, ScriptResponseOrUnhandled, SequencerLocation, Unhandled}
import esw.ocs.api.protocol.{ScriptError, SequenceComponentResponse}
import esw.sm.api.models.SequenceComponentStatus
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.StartSequencerResponse.{LoadScriptError, SequenceComponentNotAvailable, Started}
import esw.sm.api.protocol._
import esw.sm.impl.config.Sequencers
import esw.sm.impl.utils.SequenceComponentAllocator.SequencerToSequenceComponentMap

import scala.async.Async._
import scala.concurrent.Future
import Types._

class SequenceComponentUtil(locationServiceUtil: LocationServiceUtil, sequenceComponentAllocator: SequenceComponentAllocator)(
    implicit actorSystem: ActorSystem[_]
) {
  import actorSystem.executionContext

  def allocateSequenceComponents(
      sequencers: Sequencers
  ): Future[Either[ConfigureResponse.Failure, SequencerToSequenceComponentMap]] =
    getAllIdleSequenceComponentsFor(sequencers.subsystems).mapRightE(sequenceComponentAllocator.allocate(_, sequencers))

  // get all sequence components for subsystems and find idle ones from these sequence components
  private def getAllIdleSequenceComponentsFor(subsystems: List[Subsystem]) =
    locationServiceUtil
      .listAkkaLocationsBy(SequenceComponent, withFilter = location => subsystems.contains(location.prefix.subsystem))
      .flatMapRight(filterIdleSequenceComponents)
      .mapLeft(error => LocationServiceError(error.msg))

  def loadScript(subsystem: Subsystem, obsMode: ObsMode): Future[StartSequencerResponse] =
    getAllIdleSequenceComponentsFor(List(subsystem, ESW)) //search idle seq comps for ESW as fallback if needed
      .mapRightE(sequenceComponentAllocator.allocate(_, Sequencers(subsystem)))
      .flatMapE {
        case (subsystem, seqCompLocation) :: _ => loadScript(subsystem, obsMode, seqCompLocation)
        case Nil                               => Future.successful(Left(SequenceComponentNotAvailable(Nil))) // this should never happen
      }
      .mapToAdt(identity, identity)

  def loadScript(
      subsystem: Subsystem,
      obsMode: ObsMode,
      seqCompLocation: SeqCompLocation
  ): Future[Either[StartSequencerResponse.Failure, Started]] =
    sequenceComponentApi(seqCompLocation)
      .loadScript(subsystem, obsMode)
      .flatMap {
        case SequencerLocation(location)             => Future.successful(Right(Started(location.connection.componentId)))
        case error: ScriptError.LocationServiceError => Future.successful(Left(LocationServiceError(error.msg)))
        case error: ScriptError.LoadingScriptFailed  => Future.successful(Left(LoadScriptError(error.msg)))
        case error: Unhandled                        => Future.successful(Left(LoadScriptError(error.msg)))
      }

  def unloadScript(seqCompLocation: SeqCompLocation): Future[Ok.type] = sequenceComponentApi(seqCompLocation).unloadScript()

  def shutdownSequenceComponent(prefix: SeqCompPrefix): Future[ShutdownSequenceComponentResponse] =
    shutdown(prefix).mapToAdt(_ => ShutdownSequenceComponentResponse.Success, error => LocationServiceError(error.msg))

  def shutdownAllSequenceComponents(): Future[ShutdownSequenceComponentResponse] =
    shutdownAll()
      .mapRight(_ => SequenceComponentResponse.Ok)
      .mapToAdt(_ => ShutdownSequenceComponentResponse.Success, error => LocationServiceError(error.msg))

  def restartScript(loc: AkkaLocation): Future[ScriptResponseOrUnhandled] = sequenceComponentApi(loc).restartScript()

  def getSequenceComponentStatus(seqCompLocation: SeqCompLocation): Future[SequenceComponentStatus] =
    sequenceComponentApi(seqCompLocation).status.map(s =>
      SequenceComponentStatus(seqCompLocation.connection.componentId, s.response)
    )

  private def shutdown(
      prefix: SeqCompPrefix
  ): Future[Either[EswLocationError.FindLocationError, SequenceComponentResponse.Ok.type]] =
    locationServiceUtil
      .find(AkkaConnection(ComponentId(prefix, SequenceComponent)))
      .flatMapRight(shutdown)

  private def shutdownAll(): Future[Either[EswLocationError.RegistrationListingFailed, List[SequenceComponentResponse.Ok.type]]] =
    locationServiceUtil
      .listAkkaLocationsBy(SequenceComponent)
      .flatMapRight(Future.traverse(_)(shutdown))

  private def shutdown(seqCompLocation: SeqCompLocation) = sequenceComponentApi(seqCompLocation).shutdown()

  private def filterIdleSequenceComponents(seqCompLocations: List[SeqCompLocation]) =
    Future
      .traverse(seqCompLocations)(idleSequenceComponent)
      .map(_.collect { case Some(location) => location })

  private[sm] def idleSequenceComponent(seqCompLocation: SeqCompLocation): Future[Option[AkkaLocation]] =
    async {
      val isIdle = await(sequenceComponentApi(seqCompLocation).status).response.isEmpty
      if (isIdle) Some(seqCompLocation) else None
    }

  private[sm] def sequenceComponentApi(seqCompLocation: SeqCompLocation): SequenceComponentApi =
    new SequenceComponentImpl(seqCompLocation)
}
