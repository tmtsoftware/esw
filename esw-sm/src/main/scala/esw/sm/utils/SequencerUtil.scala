package esw.sm.utils

import akka.Done
import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation, Location}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.Timeouts
import esw.commons.utils.FutureEitherUtils.FutureEither
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
  var retryCount: Int               = 3

  def resolveMasterSequencerOf(observingMode: String): Future[Option[HttpLocation]] =
    locationServiceUtil.locationService
      .resolve(HttpConnection(ComponentId(Prefix(ESW, observingMode), Sequencer)), Timeouts.DefaultTimeout)

  def startSequencers(observingMode: String, requiredSequencers: Sequencers): Future[ConfigureResponse] = async {
    val spawnSequencerResponses: List[Either[SequencerError, AkkaLocation]] =
      await(Future.traverse(requiredSequencers.subsystems)(startSequencer(_, observingMode)))

    collectLefts(spawnSequencerResponses) match {
      case Nil =>
        // resolve master Sequencer and return LOCATION or FAILURE if location is not found
        await(resolveMasterSequencerOf(observingMode))
          .map(Success)
          .getOrElse(ConfigurationFailure(s"Error: ESW.$observingMode configuration failed"))

      case failedScriptResponses =>
        // todo : discuss this clean up step
        // await(shutdownSequencers(collectRights(spawnSequencerResponses))) // clean up spawned sequencers on failure
        FailedToStartSequencers(failedScriptResponses.map(_.msg).toSet)
    }
  }

  //fixme: replace Done with success type
  def checkForSequencersAvailability(sequencers: Sequencers, obsMode: String): Future[Either[SequencerError, Done.type]] = async {
    val resolvedSequencers: List[Either[EswLocationError, Boolean]] = await(
      Future
        .traverse(sequencers.subsystems)(resolveSequencers(obsMode, _))
    )

    collectLefts(resolvedSequencers) match {
      case Nil =>
        if (collectRights(resolvedSequencers).contains(false))
          Left(SequencerNotIdle(obsMode))
        else
          Right(Done)
      case _ => Left(LocationServiceError("Failed to check availability of sequencers"))
    }
  }

  def stopSequencers(sequencers: Sequencers, obsMode: String): Future[Either[EswLocationError, Done.type]] =
    Future
      .traverse(sequencers.subsystems) { s =>
        locationServiceUtil
          .resolveSequencer(s, obsMode)
          .flatMap {
            case Left(error)     => throw error
            case Right(location) => stopSequencer(location)
          }
      }
      .map(_ => Right(Done))
      .recover {
        case error: EswLocationError => Left(error)
      }

  private def stopSequencer(loc: AkkaLocation): Future[Done] =
    // get sequencer component from Sequencer and unload it.
    createSequencerClient(loc).getSequenceComponent.flatMap(sequenceComponentUtil.unloadScript)

  // Created in order to mock the behavior of sequencer API availability for unit test
  private[sm] def createSequencerClient(location: Location): SequencerApi = SequencerApiFactory.make(location)

  private def resolveSequencers(obsMode: String, subsystem: Subsystem): Future[Either[EswLocationError, Boolean]] = {
    locationServiceUtil
      .resolveSequencer(subsystem, obsMode, Timeouts.DefaultTimeout)
      .flatRight(location => createSequencerClient(location).isAvailable.map(Right(_)))
  }

  // spawn the sequencer on available SequenceComponent
  private def startSequencer(
      subSystem: Subsystem,
      observingMode: String,
      retryCount: Int = 0
  ): Future[Either[SequencerError, AkkaLocation]] = {
    sequenceComponentUtil
      .getAvailableSequenceComponent(subSystem)
      .flatMap {
        case Left(_) if retryCount > 0 => startSequencer(subSystem, observingMode, retryCount - 1)
        case Left(e)                   => Future.successful(Left(e))
        case Right(seqCompApi) =>
          seqCompApi
            .loadScript(subSystem, observingMode)
            .map(_.response match {
              case Left(e)    => Left(SequenceManagerError.LoadScriptError(e.msg))
              case Right(loc) => Right(loc)
            })
      }
  }

  private def collectLefts[L, R](responses: List[Either[L, R]]): List[L] =
    responses.collect { case Left(reasons) => reasons }

  private def collectRights[L, R](responses: List[Either[L, R]]): List[R] =
    responses.collect { case Right(values) => values }

}
