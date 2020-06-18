package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.{AkkaLocation, ComponentId, Location}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.commons.utils.FutureUtils
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.protocol.ScriptError
import esw.ocs.api.protocol.SequenceComponentResponse.{Ok, SequencerLocation, Unhandled}
import esw.ocs.api.{SequenceComponentApi, SequencerApi}
import esw.sm.api.protocol.CleanupResponse.FailedToShutdownSequencers
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ConfigureResponse.{FailedToStartSequencers, Success}
import esw.sm.api.protocol.ShutdownSequencerResponse.UnloadScriptError
import esw.sm.api.protocol.StartSequencerResponse.LoadScriptError
import esw.sm.api.protocol._
import esw.sm.impl.config.Sequencers

import scala.concurrent.{ExecutionContext, Future}

class SequencerUtil(locationServiceUtil: LocationServiceUtil, sequenceComponentUtil: SequenceComponentUtil)(implicit
    actorSystem: ActorSystem[_]
) {
  implicit private val ec: ExecutionContext = actorSystem.executionContext

  // spawn the sequencer on available SequenceComponent
  def startSequencer(
      subSystem: Subsystem,
      obsMode: String,
      retryCount: Int
  ): Future[Either[StartSequencerResponse.Failure, AkkaLocation]] =
    sequenceComponentUtil
      .getAvailableSequenceComponent(subSystem)
      .flatMap {
        case Right(seqCompApi)         => loadScript(subSystem, obsMode, seqCompApi, retryCount)
        case Left(_) if retryCount > 0 => startSequencer(subSystem, obsMode, retryCount - 1)
        case Left(e)                   => Future.successful(Left(e))
      }

  def startSequencers(obsMode: String, requiredSequencers: Sequencers, retryCount: Int): Future[ConfigureResponse] = {
    def masterSequencerId = ComponentId(Prefix(ESW, obsMode), Sequencer)

    val startSequencerResponses = sequential(requiredSequencers.subsystems)(startSequencer(_, obsMode, retryCount))
    startSequencerResponses.mapToAdt(_ => Success(masterSequencerId), e => FailedToStartSequencers(e.map(_.msg).toSet))
  }

  def shutdownSequencer(
      subsystem: Subsystem,
      obsMode: String,
      shutdownSequenceComp: Boolean = false
  ): Future[Either[ShutdownSequencerResponse.Failure, ShutdownSequencerResponse.Success.type]] = {
    locationServiceUtil
      .findSequencer(subsystem, obsMode)
      .flatMap {
        case Left(listingFailed: RegistrationListingFailed) => Future.successful(Left(LocationServiceError(listingFailed.msg)))
        case Left(LocationNotFound(_))                      => Future.successful(Right(ShutdownSequencerResponse.Success))
        case Right(sequencerLoc)                            => unloadScript(Prefix(subsystem, obsMode), sequencerLoc, shutdownSequenceComp)
      }
  }

  def shutdownSequencers(sequencers: Sequencers, obsMode: String): Future[CleanupResponse] = {
    val shutdownResponses = traverse(sequencers.subsystems)(shutdownSequencer(_, obsMode))
    shutdownResponses.mapToAdt(_ => CleanupResponse.Success, e => FailedToShutdownSequencers(e.map(_.msg).toSet))
  }

  private def shutdownSequencers(sequencerLocations: List[AkkaLocation]): Future[ShutdownAllSequencersResponse] =
    traverse(sequencerLocations)(location => unloadScript(location.prefix, location, shutdownSequenceComp = false))
      .mapToAdt(_ => ShutdownAllSequencersResponse.Success, ShutdownAllSequencersResponse.ShutdownFailure)

  def shutdownAllSequencers(): Future[ShutdownAllSequencersResponse] =
    locationServiceUtil.listAkkaLocationsBy(Sequencer).flatMapToAdt(shutdownSequencers, e => LocationServiceError(e.msg))

  def restartSequencer(subSystem: Subsystem, obsMode: String): Future[RestartSequencerResponse] =
    locationServiceUtil
      .findSequencer(subSystem, obsMode)
      .flatMapToAdt(restartSequencer, e => LocationServiceError(e.msg))

  private def restartSequencer(akkaLocation: AkkaLocation): Future[RestartSequencerResponse] =
    createSequencerClient(akkaLocation).getSequenceComponent
      .flatMap(sequenceComponentUtil.restart(_).map {
        case SequencerLocation(location) => RestartSequencerResponse.Success(location.connection.componentId)
        case error: ScriptError          => LoadScriptError(error.msg)
        case Unhandled(_, _, msg)        => LoadScriptError(msg) // restart is unhandled in idle or shutting down state
      })

  private def loadScript(
      subSystem: Subsystem,
      observingMode: String,
      seqCompApi: SequenceComponentApi,
      retryCount: Int
  ): Future[Either[StartSequencerResponse.Failure, AkkaLocation]] =
    loadScript(subSystem, observingMode, seqCompApi)
      .flatMap {
        case SequencerLocation(location)                           => Future.successful(Right(location))
        case _: ScriptError.LocationServiceError if retryCount > 0 => startSequencer(subSystem, observingMode, retryCount - 1)
        case error: ScriptError.LoadingScriptFailed                => Future.successful(Left(LoadScriptError(error.msg)))
        case Unhandled(_, _, _) if retryCount > 0                  => startSequencer(subSystem, observingMode, retryCount - 1)
      }

  private def loadScript(subSystem: Subsystem, observingMode: String, seqCompApi: SequenceComponentApi) =
    seqCompApi.loadScript(subSystem, observingMode)

  // get sequence component from Sequencer and unload sequencer script
  private def unloadScript(
      prefix: Prefix,
      sequenceLocation: AkkaLocation,
      shutdownSequenceComp: Boolean
  ): Future[Either[UnloadScriptError, ShutdownSequencerResponse.Success.type]] =
    createSequencerClient(sequenceLocation).getSequenceComponent
      .flatMap(loc =>
        sequenceComponentUtil
          .unloadScript(loc)
          .flatMap(x => if (shutdownSequenceComp) sequenceComponentUtil.shutdown(loc) else Future.successful(x))
      )
      .map {
        case Ok                   => Right(ShutdownSequencerResponse.Success)
        case Unhandled(_, _, msg) => Left(UnloadScriptError(prefix, msg))
      }
      .mapError(e => UnloadScriptError(prefix, e.getMessage))

  // Created in order to mock the behavior of sequencer API availability for unit test
  private[sm] def createSequencerClient(location: Location): SequencerApi = SequencerApiFactory.make(location)

  private def traverse[T, L, R](i: List[T])(f: T => Future[Either[L, R]])   = Future.traverse(i)(f).map(_.sequence)
  private def sequential[T, L, R](i: List[T])(f: T => Future[Either[L, R]]) = FutureUtils.sequential(i)(f).map(_.sequence)
}
