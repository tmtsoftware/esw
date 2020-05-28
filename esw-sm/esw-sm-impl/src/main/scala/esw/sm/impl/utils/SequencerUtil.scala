package esw.sm.impl.utils

import akka.Done
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
import esw.commons.utils.location.EswLocationError.{RegistrationListingFailed, LocationNotFound}
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.{SequenceComponentApi, SequencerApi}
import esw.sm.api.models.CommonFailure.LocationServiceError
import esw.sm.api.models.ConfigureResponse.{FailedToStartSequencers, Success}
import esw.sm.api.models.{ConfigureResponse, SequenceManagerError, SequencerError}
import esw.sm.impl.config.Sequencers

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class SequencerUtil(locationServiceUtil: LocationServiceUtil, sequenceComponentUtil: SequenceComponentUtil)(implicit
    actorSystem: ActorSystem[_]
) {
  implicit private val ec: ExecutionContext = actorSystem.executionContext

  //todo: Should we get it from conf
  private val retryCount: Int = 3

  private def masterSequencerConnection(obsMode: String): HttpConnection =
    HttpConnection(ComponentId(Prefix(ESW, obsMode), Sequencer))

  def resolveMasterSequencerOf(observingMode: String): Future[Either[EswLocationError, HttpLocation]] =
    locationServiceUtil.resolve(masterSequencerConnection(observingMode), Timeouts.DefaultTimeout)

  def startSequencers(observingMode: String, requiredSequencers: Sequencers): Future[ConfigureResponse] =
    async {
      val spawnSequencerResponses: Either[List[SequencerError], List[AkkaLocation]] =
        await(FutureUtils.sequential(requiredSequencers.subsystems)(startSequencer(_, observingMode, retryCount))).sequence

      spawnSequencerResponses match {
        case Left(failedScriptResponses) => FailedToStartSequencers(failedScriptResponses.map(_.msg).toSet)
        // resolve master Sequencer and return LOCATION or FAILURE if location is not found
        case Right(_) => await(resolveMasterSequencerOf(observingMode).mapToAdt(Success, err => LocationServiceError(err.msg)))
      }
    }

  def stopSequencers(sequencers: Sequencers, obsMode: String): Future[Either[RegistrationListingFailed, Done]] =
    Future
      .traverse(sequencers.subsystems) { subsystem =>
        resolveSequencer(obsMode, subsystem)
          .flatMap {
            case Left(listingFailed: RegistrationListingFailed) => throw listingFailed
            case Left(LocationNotFound(_))                      => Future.successful(Done)
            case Right(sequencerApi)                            => stopSequencer(sequencerApi)
          }
      }
      .map(_ => Right(Done))
      .recover { case listingFailed: RegistrationListingFailed => Left(listingFailed) }

  // spawn the sequencer on available SequenceComponent
  def startSequencer(
      subSystem: Subsystem,
      observingMode: String,
      retryCount: Int = retryCount
  ): Future[Either[SequencerError, AkkaLocation]] =
    sequenceComponentUtil
      .getAvailableSequenceComponent(subSystem)
      .flatMap {
        case Right(seqCompApi)         => loadScript(subSystem, observingMode, seqCompApi)
        case Left(_) if retryCount > 0 => startSequencer(subSystem, observingMode, retryCount - 1)
        case Left(e)                   => Future.successful(Left(e))
      }

  // get sequence component from Sequencer and unload it.
  private def stopSequencer(api: SequencerApi): Future[Done] =
    api.getSequenceComponent.flatMap(sequenceComponentUtil.unloadScript)

  // Created in order to mock the behavior of sequencer API availability for unit test
  private[sm] def createSequencerClient(location: Location): SequencerApi = SequencerApiFactory.make(location)
  private def resolveSequencer(obsMode: String, subsystem: Subsystem) =
    locationServiceUtil
      .resolveSequencer(subsystem, obsMode)
      .mapRight(createSequencerClient)

  private def loadScript(subSystem: Subsystem, observingMode: String, seqCompApi: SequenceComponentApi) =
    seqCompApi
      .loadScript(subSystem, observingMode)
      .map(_.response.left.map(e => SequenceManagerError.LoadScriptError(e.msg)))

}
