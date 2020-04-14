package esw.sm.utils

import akka.Done
import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, HttpLocation}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.core.Sequencers
import esw.sm.messages.ConfigureResponse
import esw.sm.messages.ConfigureResponse.{ConfigurationFailure, FailedToStartSequencers, Success}

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

case class SequencerError(msg: String)

class SequencerUtil(locationServiceUtil: LocationServiceUtil, sequenceComponentUtil: SequenceComponentUtil)(
    implicit actorSystem: ActorSystem[_]
) {
  implicit val ec: ExecutionContext = actorSystem.executionContext

  def startSequencers(observingMode: String, requiredSequencers: Sequencers)(
      implicit ec: ExecutionContext
  ): Future[ConfigureResponse] = async {
    val spawnSequencerResponsesF = Future.traverse(requiredSequencers.subsystems)(s => startSequencer(s, observingMode))
    val failureReasons           = filterFailures(await(spawnSequencerResponsesF))

    failureReasons match {
      case Nil =>
        // resolve master Sequencer and return location or failure if location is not found
        await(resolveMasterSequencer(observingMode))
          .map(Success)
          .getOrElse(ConfigurationFailure(s"Error: ESW.${observingMode} configuration failed"))

      case failedScriptResponses => FailedToStartSequencers(failedScriptResponses.map(_.msg))
    }
  }

  // spawn the sequencer on available SequenceComponent
  private def startSequencer(subSystem: Subsystem, observingMode: String): Future[Either[SequencerError, Done.type]] = {
    sequenceComponentUtil
      .getAvailableSequenceComponent(subSystem)
      .flatMap {
        case Left(value) => Future.successful(Left(value))
        case Right(value) =>
          value
            .loadScript(subSystem, observingMode)
            .map(_.response match {
              case Left(value) => Left(SequencerError(value.msg))
              case Right(_)    => Right(Done)
            })
      }
  }

  def resolveMasterSequencer(observingMode: String): Future[Option[HttpLocation]] =
    locationServiceUtil.locationService
      .resolve(HttpConnection(ComponentId(Prefix(ESW, observingMode), Sequencer)), 5.seconds)

  private def filterFailures(responses: List[Either[SequencerError, Done]]): List[SequencerError] =
    for {
      failedRes <- responses if failedRes.isLeft
      reason    <- failedRes.left.toOption
    } yield reason
}
