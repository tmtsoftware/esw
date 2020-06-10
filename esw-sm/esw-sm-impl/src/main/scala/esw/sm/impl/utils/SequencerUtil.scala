package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation, Location}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.Timeouts
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.commons.utils.FutureUtils
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.protocol.ScriptError.LoadingScriptFailed
import esw.ocs.api.{SequenceComponentApi, SequencerApi}
import esw.sm.api.models.CleanupResponse.FailedToShutdownSequencers
import esw.sm.api.models.CommonFailure.LocationServiceError
import esw.sm.api.models.ConfigureResponse.{FailedToStartSequencers, Success}
import esw.sm.api.models.SequenceManagerError.UnloadScriptError
import esw.sm.api.models._
import esw.sm.impl.config.Sequencers

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SequencerUtil(locationServiceUtil: LocationServiceUtil, sequenceComponentUtil: SequenceComponentUtil)(implicit
    actorSystem: ActorSystem[_]
) {
  implicit private val ec: ExecutionContext = actorSystem.executionContext

  private def masterSequencerConnection(obsMode: String) = HttpConnection(ComponentId(Prefix(ESW, obsMode), Sequencer))

  def resolveMasterSequencerOf(observingMode: String): Future[Either[EswLocationError, HttpLocation]] =
    locationServiceUtil.resolve(masterSequencerConnection(observingMode), Timeouts.DefaultTimeout)

  def startSequencers(observingMode: String, requiredSequencers: Sequencers, retryCount: Int): Future[ConfigureResponse] =
    async {
      def masterSequencerId = ComponentId(Prefix(ESW, observingMode), Sequencer)

      val spawnSequencerResponses =
        await(FutureUtils.sequential(requiredSequencers.subsystems)(startSequencer(_, observingMode, retryCount))).sequence

      spawnSequencerResponses match {
        case Left(failedScriptResponses) => FailedToStartSequencers(failedScriptResponses.map(_.msg).toSet)
        case Right(_)                    => Success(masterSequencerId)
      }
    }

  def shutdownSequencers(sequencers: Sequencers, obsMode: String): Future[CleanupResponse] =
    async {
      val shutdownSequencerResponses = await(Future.traverse(sequencers.subsystems) { subsystem =>
        shutdownSequencer(subsystem, obsMode)
      }).sequence

      shutdownSequencerResponses match {
        case Left(failedToShutdownSequencerResponses) =>
          FailedToShutdownSequencers(failedToShutdownSequencerResponses.map(_.msg).toSet)
        case Right(_) => CleanupResponse.Success
      }
    }

  def shutdownSequencer(
      subsystem: Subsystem,
      obsMode: String
  ): Future[Either[ShutdownSequencerResponse.Failure, ShutdownSequencerResponse.Success.type]] = {
    resolveSequencer(obsMode, subsystem)
      .flatMap {
        case Left(listingFailed: RegistrationListingFailed) => Future.successful(Left(LocationServiceError(listingFailed.msg)))
        case Left(LocationNotFound(_))                      => Future.successful(Right(ShutdownSequencerResponse.Success))
        case Right(sequencerApi)                            => unloadScript(Prefix(subsystem, obsMode), sequencerApi)
      }
  }

  def shutdownAllSequencers(): Future[ShutdownAllSequencersResponse] = {

    locationServiceUtil.listAkkaLocationsBy(Sequencer).flatMap {
      case Left(listingFailed: RegistrationListingFailed) => Future.successful(LocationServiceError(listingFailed.msg))
      case Right(sequencerLocations) =>
        Future
          .traverse(sequencerLocations) { location =>
            unloadScript(location.prefix, createSequencerClient(location))
          }
          .map(_.sequence)
          .map {
            case Left(value) => ShutdownAllSequencersResponse.ShutDownFailure(value)
            case Right(_)    => ShutdownAllSequencersResponse.Success
          }
    }
  }

  // spawn the sequencer on available SequenceComponent
  def startSequencer(
      subSystem: Subsystem,
      obsMode: String,
      retryCount: Int
  ): Future[Either[SequencerError, AkkaLocation]] =
    sequenceComponentUtil
      .getAvailableSequenceComponent(subSystem)
      .flatMap {
        case Right(seqCompApi)         => loadScript(subSystem, obsMode, seqCompApi, retryCount)
        case Left(_) if retryCount > 0 => startSequencer(subSystem, obsMode, retryCount - 1)
        case Left(e)                   => Future.successful(Left(e))
      }

  def restartSequencer(
      subSystem: Subsystem,
      obsMode: String,
      retryCount: Int
  ): Future[Either[RestartSequencerResponse.Failure, AkkaLocation]] = {
    shutdownSequencer(subSystem, obsMode).flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(_)    => startSequencer(subSystem, obsMode, retryCount).mapRight(identity)
    }
  }

  private def loadScript(
      subSystem: Subsystem,
      observingMode: String,
      seqCompApi: SequenceComponentApi,
      retryCount: Int
  ): Future[Either[SequencerError, AkkaLocation]] = {
    seqCompApi
      .loadScript(subSystem, observingMode)
      .map(_.response)
      .flatMap {
        case Left(error: LoadingScriptFailed) => Future.successful(Left(SequenceManagerError.LoadScriptError(error.msg)))
        case Left(_) if retryCount > 0        => startSequencer(subSystem, observingMode, retryCount - 1)
        case Right(location)                  => Future.successful(Right(location))
      }
  }

  // get sequence component from Sequencer and unload sequencer script
  private def unloadScript(
      prefix: Prefix,
      api: SequencerApi
  ): Future[Either[UnloadScriptError, ShutdownSequencerResponse.Success.type]] =
    api.getSequenceComponent
      .flatMap(sequenceComponentUtil.unloadScript)
      .map(_ => Right(ShutdownSequencerResponse.Success))
      .recover {
        case NonFatal(e) => Left(UnloadScriptError(prefix, e.getMessage))
      }

  // Created in order to mock the behavior of sequencer API availability for unit test
  private[sm] def createSequencerClient(location: Location): SequencerApi = SequencerApiFactory.make(location)
  private def resolveSequencer(obsMode: String, subsystem: Subsystem) =
    locationServiceUtil
      .resolveSequencer(subsystem, obsMode, 3.seconds)
      .mapRight(createSequencerClient)

}
