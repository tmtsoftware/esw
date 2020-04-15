package esw.sm.utils

import akka.Done
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.actor.client.{SequenceComponentImpl, SequencerApiFactory}
import esw.sm.core.Sequencers
import esw.sm.messages.ConfigureResponse
import esw.sm.messages.ConfigureResponse.{ConfigurationFailure, FailedToStartSequencers, Success}

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

case class SequencerError(msg: String)

class SequencerUtil(locationServiceUtil: LocationServiceUtil, sequenceComponentUtil: SequenceComponentUtil)(
    implicit actorSystem: ActorSystem[_],
    timeout: Timeout
) {
  implicit val ec: ExecutionContext = actorSystem.executionContext

  def resolveMasterSequencerOf(observingMode: String): Future[Option[HttpLocation]] =
    locationServiceUtil.locationService
      .resolve(HttpConnection(ComponentId(Prefix(ESW, observingMode), Sequencer)), 5.seconds)

  def startSequencers(observingMode: String, requiredSequencers: Sequencers)(
      implicit ec: ExecutionContext
  ): Future[ConfigureResponse] = async {
    val spawnSequencerResponses: List[Either[SequencerError, AkkaLocation]] =
      await(Future.traverse(requiredSequencers.subsystems)(startSequencer(_, observingMode)))

    collectLefts(spawnSequencerResponses) match {
      case Nil =>
        // resolve master Sequencer and return LOCATION or FAILURE if location is not found
        await(resolveMasterSequencerOf(observingMode))
          .map(Success)
          .getOrElse(ConfigurationFailure(s"Error: ESW.${observingMode} configuration failed"))

      case failedScriptResponses =>
        await(shutdownSequencers(collectRights(spawnSequencerResponses))) // clean up spawned sequencers on failure
        FailedToStartSequencers(failedScriptResponses.map(_.msg).toSet)
    }
  }

  // spawn the sequencer on available SequenceComponent
  private def startSequencer(subSystem: Subsystem, observingMode: String): Future[Either[SequencerError, AkkaLocation]] = {
    sequenceComponentUtil
      .getAvailableSequenceComponent(subSystem)
      .flatMap {
        case Left(e) => Future.successful(Left(e))
        case Right(seqCompApi) =>
          seqCompApi
            .loadScript(subSystem, observingMode)
            .map(_.response match {
              case Left(e)    => Left(SequencerError(e.msg))
              case Right(loc) => Right(loc)
            })
      }
  }

  private def shutdownSequencers(sequencers: List[AkkaLocation]): Future[Done] =
    Future.traverse(sequencers)(shutdownSequencer).map(_ => Done)

  private def shutdownSequencer(loc: AkkaLocation): Future[Done] =
    // get sequencer component from Sequencer and unload it.
    SequencerApiFactory.make(loc).getSequenceComponent.flatMap(new SequenceComponentImpl(_).unloadScript())

  private def collectLefts(responses: List[Either[SequencerError, AkkaLocation]]): List[SequencerError] =
    responses.collect { case Left(reasons) => reasons }

  private def collectRights(responses: List[Either[SequencerError, AkkaLocation]]): List[AkkaLocation] =
    responses.collect { case Right(location) => location }
}
