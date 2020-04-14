package esw.sm.utils

import akka.Done
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, HttpLocation}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.SequenceComponentApi
import esw.ocs.impl.internal.LocationServiceUtil
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
  private def startSequencer(subSystem: Subsystem, observingMode: String): Future[Either[SequencerError, Done]] =
    for {
      mayBeSeqComp                     <- sequenceComponentUtil.getAvailableSequenceComponent(subSystem)
      seqCompApi: SequenceComponentApi = mayBeSeqComp
      res                              <- seqCompApi.loadScript(subSystem, observingMode)
    } yield res.response match {
      case Left(value) => Left(SequencerError(value.msg))
      case _           => Right(Done)
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
