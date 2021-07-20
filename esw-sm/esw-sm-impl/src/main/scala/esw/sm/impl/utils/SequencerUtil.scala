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
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.ScriptError
import esw.ocs.api.protocol.SequenceComponentResponse.{SequencerLocation, Unhandled}
import esw.sm.api.models.Sequencers
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ConfigureResponse.FailedToStartSequencers
import esw.sm.api.protocol.StartSequencerResponse.LoadScriptError
import esw.sm.api.protocol._
import esw.sm.impl.utils.Types.SeqCompLocation

import scala.concurrent.{ExecutionContext, Future}

class SequencerUtil(locationServiceUtil: LocationServiceUtil, sequenceComponentUtil: SequenceComponentUtil)(implicit
    actorSystem: ActorSystem[_]
) {
  implicit private val ec: ExecutionContext = actorSystem.executionContext

  def startSequencers(obsMode: ObsMode, sequencers: Sequencers): Future[ConfigureResponse] =
    sequenceComponentUtil
      .allocateSequenceComponents(sequencers)
      .flatMapRight(startSequencersByMapping(obsMode, _)) // load scripts for sequencers on mapped sequence components
      .mapToAdt(identity, identity)

  def restartSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[RestartSequencerResponse] =
    locationServiceUtil
      .findSequencer(subsystem, obsMode.name)
      .flatMapToAdt(restartSequencer, e => LocationServiceError(e.msg))

  def shutdownSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[ShutdownSequencersResponse] =
    shutdownSequencersAndHandleErrors(getSequencer(subsystem, obsMode))
  def shutdownSubsystemSequencers(subsystem: Subsystem): Future[ShutdownSequencersResponse] =
    shutdownSequencersAndHandleErrors(getSubsystemSequencers(subsystem))
  def shutdownObsModeSequencers(obsMode: ObsMode): Future[ShutdownSequencersResponse] =
    shutdownSequencersAndHandleErrors(getObsModeSequencers(obsMode))
  def shutdownAllSequencers(): Future[ShutdownSequencersResponse] = shutdownSequencersAndHandleErrors(getAllSequencers)

  private[utils] def startSequencersByMapping(
      obsMode: ObsMode,
      mappings: List[(Subsystem, SeqCompLocation)]
  ): Future[ConfigureResponse] =
    Future
      .traverse(mappings) { case (subsystem, seqCompLocation) =>
        sequenceComponentUtil.loadScript(subsystem, obsMode, seqCompLocation)
      }
      .map(_.sequence)
      .mapToAdt(
        _ => ConfigureResponse.Success(ComponentId(Prefix(ESW, obsMode.name), Sequencer)),
        errors => FailedToStartSequencers(errors.map(_.msg).toSet)
      )

  private def restartSequencer(sequencerLocation: AkkaLocation) =
    makeSequencerClient(sequencerLocation).getSequenceComponent
      .flatMap(sequenceComponentUtil.restartScript(_).map {
        case SequencerLocation(location) => RestartSequencerResponse.Success(location.connection.componentId)
        case error: ScriptError          => LoadScriptError(error.msg)
        case Unhandled(_, _, msg)        => LoadScriptError(msg) // restart is unhandled in idle or shutting down state
      })

  private def getSequencer(subsystem: Subsystem, obsMode: ObsMode) =
    locationServiceUtil.findSequencer(subsystem, obsMode.name).mapRight(List(_))
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
