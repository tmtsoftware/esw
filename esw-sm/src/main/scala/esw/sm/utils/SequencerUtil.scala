package esw.sm.utils

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
import esw.commons.utils.location.EswLocationError.{RegistrationListingFailed, ResolveLocationFailed}
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.sm.core.Sequencers
import esw.sm.messages.ConfigureResponse
import esw.sm.messages.ConfigureResponse.{ConfigurationFailure, FailedToStartSequencers, Success}
import esw.sm.utils.SequenceManagerError.{LocationServiceError, SequencerNotIdle}

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class SequencerUtil(locationServiceUtil: LocationServiceUtil, sequenceComponentUtil: SequenceComponentUtil)(
    implicit actorSystem: ActorSystem[_]
) {
  implicit val ec: ExecutionContext = actorSystem.executionContext

  //todo: Should we get it from conf
  private val retryCount: Int = 3

  private def masterSequencerConnection(obsMode: String): HttpConnection =
    HttpConnection(ComponentId(Prefix(ESW, obsMode), Sequencer))

  def resolveMasterSequencerOf(observingMode: String): Future[Either[EswLocationError, HttpLocation]] =
    locationServiceUtil.resolve(masterSequencerConnection(observingMode), Timeouts.DefaultTimeout)

  def startSequencers(observingMode: String, requiredSequencers: Sequencers): Future[ConfigureResponse] = async {
    val spawnSequencerResponses: Either[List[SequencerError], List[AkkaLocation]] =
      await(Future.traverse(requiredSequencers.subsystems)(startSequencer(_, observingMode, retryCount))).sequence

    spawnSequencerResponses match {
      case Left(failedScriptResponses) =>
        // todo : discuss this clean up step
        // await(shutdownSequencers(collectRights(spawnSequencerResponses))) // clean up spawned sequencers on failure
        FailedToStartSequencers(failedScriptResponses.map(_.msg).toSet)

      case Right(_) =>
        // resolve master Sequencer and return LOCATION or FAILURE if location is not found
        await(resolveMasterSequencerOf(observingMode)) match {
          case Left(error)                    => ConfigurationFailure(error.msg)
          case Right(masterSequencerLocation) => Success(masterSequencerLocation)
        }
    }
  }

  //fixme: replace Done with success type
  def checkForSequencersAvailability(sequencers: Sequencers, obsMode: String): Future[Either[SequencerError, Done]] = async {
    val resolvedSequencers: Either[List[EswLocationError], List[Boolean]] =
      await(Future.traverse(sequencers.subsystems)(resolveSequencers(obsMode, _))).sequence

    resolvedSequencers match {
      case Right(bools) if bools.contains(false) => Left(SequencerNotIdle(obsMode))
      case Right(_)                              => Right(Done)
      case Left(_)                               => Left(LocationServiceError("Failed to check availability of sequencers"))
    }
  }

  def stopSequencers(sequencers: Sequencers, obsMode: String): Future[Either[RegistrationListingFailed, Done]] = {
    Future
      .traverse(sequencers.subsystems) { subsystem =>
        locationServiceUtil
          .resolveSequencer(subsystem, obsMode)
          .map {
            case Left(error)     => Left(error)
            case Right(location) => Right(stopSequencer(location))
          }
      }
      .flatMap(statusList => {
        Future
          .traverse(statusList.map {
            case Left(RegistrationListingFailed(msg)) => throw RegistrationListingFailed(msg)
            case Left(ResolveLocationFailed(_))       => Future.successful(Done)
            case Right(value)                         => value.map(_ => Done)
          })(x => x)

      })
      .map(_ => Right(Done))
      .recover {
        case RegistrationListingFailed(msg) => Left(RegistrationListingFailed(msg))
      }
  }

  // get sequence component from Sequencer and unload it.
  private def stopSequencer(loc: AkkaLocation): Future[Done] =
    createSequencerClient(loc).getSequenceComponent.flatMap(sequenceComponentUtil.unloadScript)

  // Created in order to mock the behavior of sequencer API availability for unit test
  private[sm] def createSequencerClient(location: Location): SequencerApi = SequencerApiFactory.make(location)

  private def resolveSequencers(obsMode: String, subsystem: Subsystem): Future[Either[EswLocationError, Boolean]] =
    locationServiceUtil
      .resolveSequencer(subsystem, obsMode, Timeouts.DefaultTimeout)
      .flatMapRight(location => createSequencerClient(location).isAvailable)

  // spawn the sequencer on available SequenceComponent
  private def startSequencer(
      subSystem: Subsystem,
      observingMode: String,
      retryCount: Int
  ): Future[Either[SequencerError, AkkaLocation]] = {
    sequenceComponentUtil
      .getAvailableSequenceComponent(subSystem)
      .flatMap {
        case Left(_) if retryCount > 0 => startSequencer(subSystem, observingMode, retryCount - 1)
        case Left(e)                   => Future.successful(Left(e))
        case Right(seqCompApi) =>
          seqCompApi
            .loadScript(subSystem, observingMode)
            .map(_.response.left.map(e => SequenceManagerError.LoadScriptError(e.msg)))
      }
  }
}
