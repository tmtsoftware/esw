package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.ESW
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.api.protocol.SequenceComponentResponse.{Ok, ScriptResponseOrUnhandled, SequencerLocation, Unhandled}
import esw.ocs.api.protocol.{ScriptError, SequenceComponentResponse}
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.StartSequencerResponse.{LoadScriptError, SequenceComponentNotAvailable, Started}
import esw.sm.api.protocol.{ConfigureResponse, ShutdownSequenceComponentResponse, StartSequencerResponse}
import esw.sm.impl.utils.SequenceComponentAllocator.SequencerToSequenceComponentMap
import esw.sm.impl.utils.Types.*

import scala.async.Async.*
import scala.concurrent.Future

class SequenceComponentUtil(locationServiceUtil: LocationServiceUtil, val sequenceComponentAllocator: SequenceComponentAllocator)(
    implicit actorSystem: ActorSystem[_]
) {
  import actorSystem.executionContext

  def allocateSequenceComponents(
      sequencerPrefixes: List[SequencerPrefix]
  ): Future[Either[ConfigureResponse.Failure, SequencerToSequenceComponentMap]] =
    getAllIdleSequenceComponentsFor(sequencerPrefixes.map(_.subsystem))
      .mapRightE(sequenceComponentAllocator.allocate(_, sequencerPrefixes))

  def getAllIdleSequenceComponents: Future[Either[LocationServiceError, List[SeqCompLocation]]] =
    locationServiceUtil
      .listAkkaLocationsBy(SequenceComponent)
      .flatMapRight(filterIdleSequenceComponents)
      .mapLeft(error => LocationServiceError(error.msg))

  // get all sequence components for subsystems and find idle ones from these sequence components
  private def getAllIdleSequenceComponentsFor(subsystems: List[Subsystem]) =
    getAllIdleSequenceComponents.mapRight(seqCompLocs => seqCompLocs.filter(loc => subsystems.contains(loc.prefix.subsystem)))

  def loadScript(prefix: SequencerPrefix): Future[StartSequencerResponse] = {
    getAllIdleSequenceComponentsFor(List(prefix.subsystem, ESW)) //search idle seq comps for ESW as fallback if needed
      .mapRightE(sequenceComponentAllocator.allocate(_, List(prefix)))
      .flatMapE {
        case (sequencersWithMayBeVariation, seqCompLocation) :: _ =>
          loadScript(prefix, seqCompLocation)
        case Nil => Future.successful(Left(SequenceComponentNotAvailable(Nil))) // this should never happen
      }
      .mapToAdt(identity, identity)
  }

  def loadScript(
      prefix: SequencerPrefix,
      seqCompLocation: SeqCompLocation
  ): Future[Either[StartSequencerResponse.Failure, Started]] =
    sequenceComponentApi(seqCompLocation)
      .loadScript(prefix)
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

  private def shutdown(
      prefix: SeqCompPrefix
  ): Future[Either[EswLocationError.FindLocationError, SequenceComponentResponse.Ok.type]] =
    locationServiceUtil
      .find(AkkaConnection(ComponentId(prefix, SequenceComponent)))
      .flatMapRight(shutdown)

  //shuts down all the running sequence components
  private def shutdownAll(): Future[Either[EswLocationError.RegistrationListingFailed, List[SequenceComponentResponse.Ok.type]]] =
    locationServiceUtil
      .listAkkaLocationsBy(SequenceComponent)
      .flatMapRight(Future.traverse(_)(shutdown))

  //shuts down the sequence component of given location
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
