package esw.sm.impl.utils

import org.apache.pekko.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoLocation, ComponentId}
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.ESW
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.api.models.{ObsMode, Variation, VariationInfo}
import esw.ocs.api.protocol.SequenceComponentResponse.{Ok, ScriptResponseOrUnhandled, SequencerLocation, Unhandled}
import esw.ocs.api.protocol.{ScriptError, SequenceComponentResponse}
import esw.sm.api.models.VariationInfos
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.StartSequencerResponse.{LoadScriptError, SequenceComponentNotAvailable, Started}
import esw.sm.api.protocol.{ConfigureResponse, ShutdownSequenceComponentResponse, StartSequencerResponse}
import esw.sm.impl.utils.SequenceComponentAllocator.SequencerToSequenceComponentMap
import esw.sm.impl.utils.Types.*

import cps.compat.FutureAsync.*
import scala.concurrent.Future

class SequenceComponentUtil(locationServiceUtil: LocationServiceUtil, val sequenceComponentAllocator: SequenceComponentAllocator)(
    implicit actorSystem: ActorSystem[_]
) {
  import actorSystem.executionContext

  def allocateSequenceComponents(
      obsMode: ObsMode,
      variationInfos: List[VariationInfo]
  ): Future[Either[ConfigureResponse.Failure, SequencerToSequenceComponentMap]] =
    getAllIdleSequenceComponentsFor(variationInfos.map(_.subsystem))
      .mapRightE(sequenceComponentAllocator.allocate(_, obsMode, variationInfos))

  def getAllIdleSequenceComponents: Future[Either[LocationServiceError, List[SeqCompLocation]]] =
    locationServiceUtil
      .listPekkoLocationsBy(SequenceComponent)
      .flatMapRight(filterIdleSequenceComponents)
      .mapLeft(error => LocationServiceError(error.msg))

  // get all sequence components for subsystems and find idle ones from these sequence components
  private def getAllIdleSequenceComponentsFor(subsystems: List[Subsystem]) =
    getAllIdleSequenceComponents.mapRight(seqCompLocs => seqCompLocs.filter(loc => subsystems.contains(loc.prefix.subsystem)))

  def loadScript(subsystem: Subsystem, obsMode: ObsMode, variation: Option[Variation]): Future[StartSequencerResponse] = {
    getAllIdleSequenceComponentsFor(List(subsystem, ESW)) // search idle seq comps for ESW as fallback if needed
      .mapRightE(sequenceComponentAllocator.allocate(_, obsMode, List(VariationInfo(subsystem, variation))))
      .flatMapE {
        case (variationInfo, seqCompLocation) :: _ =>
          loadScript(variationInfo.subsystem, obsMode, variationInfo.variation, seqCompLocation)
        case Nil => Future.successful(Left(SequenceComponentNotAvailable(VariationInfos.empty))) // this should never happen
      }
      .mapToAdt(identity, identity)
  }

  def loadScript(
      subsystem: Subsystem,
      obsMode: ObsMode,
      variation: Option[Variation],
      seqCompLocation: SeqCompLocation
  ): Future[Either[StartSequencerResponse.Failure, Started]] = {
    sequenceComponentApi(seqCompLocation)
      .loadScript(subsystem, obsMode, variation)
      .flatMap {
        case SequencerLocation(location)             => Future.successful(Right(Started(location.connection.componentId)))
        case error: ScriptError.LocationServiceError => Future.successful(Left(LocationServiceError(error.msg)))
        case error: ScriptError.LoadingScriptFailed  => Future.successful(Left(LoadScriptError(error.msg)))
        case error: Unhandled                        => Future.successful(Left(LoadScriptError(error.msg)))
      }
  }

  def unloadScript(seqCompLocation: SeqCompLocation): Future[Ok.type] = sequenceComponentApi(seqCompLocation).unloadScript()

  def shutdownSequenceComponent(prefix: SeqCompPrefix): Future[ShutdownSequenceComponentResponse] =
    shutdown(prefix).mapToAdt(_ => ShutdownSequenceComponentResponse.Success, error => LocationServiceError(error.msg))

  def shutdownAllSequenceComponents(): Future[ShutdownSequenceComponentResponse] =
    shutdownAll()
      .mapRight(_ => SequenceComponentResponse.Ok)
      .mapToAdt(_ => ShutdownSequenceComponentResponse.Success, error => LocationServiceError(error.msg))

  def restartScript(loc: PekkoLocation): Future[ScriptResponseOrUnhandled] = sequenceComponentApi(loc).restartScript()

  private def shutdown(
      prefix: SeqCompPrefix
  ): Future[Either[EswLocationError.FindLocationError, SequenceComponentResponse.Ok.type]] =
    locationServiceUtil
      .find(PekkoConnection(ComponentId(prefix, SequenceComponent)))
      .flatMapRight(shutdown)

  // shuts down all the running sequence components
  private def shutdownAll(): Future[Either[EswLocationError.RegistrationListingFailed, List[SequenceComponentResponse.Ok.type]]] =
    locationServiceUtil
      .listPekkoLocationsBy(SequenceComponent)
      .flatMapRight(Future.traverse(_)(shutdown))

  // shuts down the sequence component of given location
  private def shutdown(seqCompLocation: SeqCompLocation) = sequenceComponentApi(seqCompLocation).shutdown()

  private def filterIdleSequenceComponents(seqCompLocations: List[SeqCompLocation]) =
    Future
      .traverse(seqCompLocations)(idleSequenceComponent)
      .map(_.collect { case Some(location) => location })

  private[sm] def idleSequenceComponent(seqCompLocation: SeqCompLocation): Future[Option[PekkoLocation]] =
    async {
      val isIdle = await(sequenceComponentApi(seqCompLocation).status).response.isEmpty
      if (isIdle) Some(seqCompLocation) else None
    }

  private[sm] def sequenceComponentApi(seqCompLocation: SeqCompLocation): SequenceComponentApi =
    new SequenceComponentImpl(seqCompLocation)
}
