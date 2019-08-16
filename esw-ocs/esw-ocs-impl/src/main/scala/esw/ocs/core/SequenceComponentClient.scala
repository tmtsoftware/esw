package esw.ocs.core

import akka.Done
import akka.actor.Scheduler
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import esw.ocs.api.models.messages.SequenceComponentMsg
import esw.ocs.api.models.messages.SequenceComponentMsg.{GetStatus, LoadScript, UnloadScript}
import esw.ocs.api.models.messages.SequenceComponentResponse.{GetStatusResponse, LoadScriptResponse}

import scala.concurrent.Future

// fixme: can this take AkkaLocation similar to other wrappers like CommandService?
class SequenceComponentClient(sequenceComponentRef: ActorRef[SequenceComponentMsg])(
    implicit scheduler: Scheduler,
    timeout: Timeout
) {
  def loadScript(
      sequencerId: String,
      observingMode: String
  ): Future[LoadScriptResponse] = sequenceComponentRef ? (x => LoadScript(sequencerId, observingMode, x))

  def getStatus: Future[GetStatusResponse] = sequenceComponentRef ? GetStatus

  def unloadScript(): Future[Done] = sequenceComponentRef ? UnloadScript
}
