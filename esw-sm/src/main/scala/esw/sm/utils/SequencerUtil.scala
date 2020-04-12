package esw.sm.utils

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, HttpLocation}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.protocol.{ScriptError, ScriptResponse}
import esw.ocs.impl.internal.LocationServiceUtil
import esw.sm.core.Sequencers
import esw.sm.messages.ConfigureResponse
import esw.sm.messages.ConfigureResponse.{ConfigurationFailure, FailedToStartSequencers, Success}

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class SequencerUtil(locationServiceUtil: LocationServiceUtil, sequenceComponentUtil: SequenceComponentUtil)(
    implicit actorSystem: ActorSystem[_],
    timeout: Timeout) {
  implicit val ec: ExecutionContext = actorSystem.executionContext

  def startSequencers(
      observingMode: String,
      requiredSequencers: Sequencers
  )(implicit ec: ExecutionContext): Future[ConfigureResponse] = async {
    val spawnSequencerResponsesF = Future.traverse(requiredSequencers.subsystems) { s =>
      sequenceComponentUtil
        .getAvailableSequenceComponent(s)
        .flatMap(_.loadScript(s, observingMode)) // spawn the sequencer on available SequenceComponent
    }
    val failureReasons = getFailedResponses(await(spawnSequencerResponsesF))

    failureReasons match {
      case Nil =>
        // resolve master Sequencer and return failure if it is not found in Location service
        await(resolveMasterSequencer(observingMode))
          .map(Success)
          .getOrElse(ConfigurationFailure(s"Error: ESW.${observingMode} configuration failed"))

      case failedScriptResponses => FailedToStartSequencers(failedScriptResponses)
    }
  }

  private def resolveMasterSequencer(observingMode: String): Future[Option[HttpLocation]] =
    locationServiceUtil.locationService
      .resolve(HttpConnection(ComponentId(Prefix(ESW, observingMode), Sequencer)), 5.seconds)

  private def getFailedResponses(responses: List[ScriptResponse]): List[ScriptError] =
    for {
      failedResponse <- responses.filter(_.response.isLeft)
      // left is safe here as we are filtering only left above
      reason <- failedResponse.response.left.toOption
    } yield reason
}
