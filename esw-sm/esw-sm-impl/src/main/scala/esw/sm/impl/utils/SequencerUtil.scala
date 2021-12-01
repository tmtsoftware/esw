package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.{AkkaLocation, ComponentId, Location}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.models.{ObsMode, VariationId}
import esw.ocs.api.protocol.ScriptError
import esw.ocs.api.protocol.SequenceComponentResponse.{SequencerLocation, Unhandled}
import esw.sm.api.models.VariationIds
import esw.sm.api.protocol.*
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ConfigureResponse.FailedToStartSequencers
import esw.sm.api.protocol.StartSequencerResponse.LoadScriptError
import esw.sm.impl.utils.Types.{SeqCompLocation, SequencerPrefix}

import scala.concurrent.{ExecutionContext, Future}

class SequencerUtil(locationServiceUtil: LocationServiceUtil, sequenceComponentUtil: SequenceComponentUtil)(implicit
    actorSystem: ActorSystem[_]
) {
  implicit private val ec: ExecutionContext = actorSystem.executionContext

  def startSequencers(obsMode: ObsMode, variationIds: VariationIds): Future[ConfigureResponse] =
    sequenceComponentUtil
      .allocateSequenceComponents(obsMode, variationIds.variationIds)
      .flatMapRight(startSequencersByMapping(obsMode, _)) // load scripts for sequencers on mapped sequence components
      .mapToAdt(identity, identity)

  def restartSequencer(prefix: SequencerPrefix): Future[RestartSequencerResponse] =
    locationServiceUtil
      .findSequencer(prefix)
      .flatMapToAdt(restartSequencer, e => LocationServiceError(e.msg))

  def shutdownSequencer(prefix: SequencerPrefix): Future[ShutdownSequencersResponse] =
    shutdownSequencersAndHandleErrors(getSequencer(prefix))

  def shutdownSubsystemSequencers(subsystem: Subsystem): Future[ShutdownSequencersResponse] =
    shutdownSequencersAndHandleErrors(getSubsystemSequencers(subsystem))

  def shutdownObsModeSequencers(obsMode: ObsMode): Future[ShutdownSequencersResponse] =
    shutdownSequencersAndHandleErrors(getObsModeSequencers(obsMode))

  def shutdownAllSequencers(): Future[ShutdownSequencersResponse] = shutdownSequencersAndHandleErrors(getAllSequencers)

  private[utils] def startSequencersByMapping(obsMode: ObsMode, mappings: List[(VariationId, SeqCompLocation)]) = {
    Future
      .traverse(mappings) { case (variationId, seqCompLocation) =>
        sequenceComponentUtil.loadScript(variationId.subsystem, obsMode, variationId.variation, seqCompLocation)
      }
      .map(_.sequence)
      .mapToAdt(
        _ => ConfigureResponse.Success(ComponentId(Prefix(ESW, obsMode.name), Sequencer)), //TODO get top level seq from mappings
        errors => FailedToStartSequencers(errors.map(_.msg).toSet)
      )
  }

  private def restartSequencer(sequencerLocation: AkkaLocation) =
    makeSequencerClient(sequencerLocation).getSequenceComponent
      .flatMap(sequenceComponentUtil.restartScript(_).map {
        case SequencerLocation(location) => RestartSequencerResponse.Success(location.connection.componentId)
        case error: ScriptError          => LoadScriptError(error.msg)
        case Unhandled(_, _, msg)        => LoadScriptError(msg) // restart is unhandled in idle or shutting down state
      })

  private def getSequencer(prefix: SequencerPrefix): Future[Either[EswLocationError.FindLocationError, List[SeqCompLocation]]] =
    locationServiceUtil.findSequencer(prefix).mapRight(List(_))
  private def getSubsystemSequencers(subsystem: Subsystem) = locationServiceUtil.listAkkaLocationsBy(subsystem, Sequencer)
  private def getObsModeSequencers(obsMode: ObsMode)       = locationServiceUtil.listAkkaLocationsBy(obsMode.name, Sequencer)
  private def getAllSequencers                             = locationServiceUtil.listAkkaLocationsBy(Sequencer)

  private def shutdownSequencersAndHandleErrors(sequencers: Future[Either[EswLocationError, List[AkkaLocation]]]) =
    sequencers.flatMapRight(unloadScripts).mapToAdt(identity, locationErrorToShutdownSequencersResponse)

  private def locationErrorToShutdownSequencersResponse(err: EswLocationError) =
    err match {
      case _: EswLocationError.LocationNotFound => ShutdownSequencersResponse.Success
      case e: EswLocationError                  => LocationServiceError(e.msg)
    }

  // get sequence component from Sequencer and unload sequencer script
  private def unloadScript(sequencerLocation: AkkaLocation) =
    makeSequencerClient(sequencerLocation).getSequenceComponent
      .flatMap(sequenceComponentUtil.unloadScript)
      .map(_ => ShutdownSequencersResponse.Success)

  private def unloadScripts(sequencerLocations: List[AkkaLocation]) =
    Future.traverse(sequencerLocations)(unloadScript).map(_ => ShutdownSequencersResponse.Success)

  // Created in order to mock the behavior of sequencer API availability for unit test
  private[sm] def makeSequencerClient(sequencerLocation: Location): SequencerApi = SequencerApiFactory.make(sequencerLocation)
}
