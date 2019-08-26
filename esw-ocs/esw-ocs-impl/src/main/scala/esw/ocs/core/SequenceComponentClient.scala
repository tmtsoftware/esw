package esw.ocs.core

import akka.Done
import akka.actor.Scheduler
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import esw.ocs.api.models.responses.SequenceComponentResponse.{GetStatusResponse, LoadScriptResponse}
import esw.ocs.core.messages.SequenceComponentMsg
import esw.ocs.core.messages.SequenceComponentMsg.{GetStatus, LoadScript, UnloadScript}

import scala.concurrent.Future

// fixme: can this take AkkaLocation similar to other wrappers like CommandService?
class SequenceComponentClient(sequenceComponentRef: ActorRef[SequenceComponentMsg])(
    implicit scheduler: Scheduler,
    timeout: Timeout
) {
  def loadScript(sequencerId: String, observingMode: String): Future[LoadScriptResponse] =
    sequenceComponentRef ? (LoadScript(sequencerId, observingMode, _))

  def getStatus: Future[GetStatusResponse] = sequenceComponentRef ? GetStatus

  def unloadScript(): Future[Done] = sequenceComponentRef ? UnloadScript
}
