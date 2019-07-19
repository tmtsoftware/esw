package esw.ocs.core

import akka.Done
import akka.actor.Scheduler
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.location.model.scaladsl.AkkaLocation
import esw.ocs.api.models.messages.SequenceComponentMsg
import esw.ocs.api.models.messages.SequenceComponentMsg.{GetStatus, LoadScript, UnloadScript}
import esw.ocs.api.models.messages.error.LoadScriptError

import scala.concurrent.Future

class SequenceComponentClient(sequenceComponentRef: ActorRef[SequenceComponentMsg])(
    implicit scheduler: Scheduler,
    timeout: Timeout
) {
  def loadScript(
      sequencerId: String,
      observingMode: String
  ): Future[Either[LoadScriptError, AkkaLocation]] = sequenceComponentRef ? (x => LoadScript(sequencerId, observingMode, x))

  def getStatus: Future[Option[AkkaLocation]] = sequenceComponentRef ? GetStatus

  def unloadScript(): Future[Done] = sequenceComponentRef ? UnloadScript
}
