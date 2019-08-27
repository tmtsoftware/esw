package esw.ocs.client

import akka.actor.Scheduler
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.models.responses.SequenceComponentResponse.{Done, GetStatusResponse, LoadScriptResponse}
import esw.ocs.client.messages.SequenceComponentMsg
import esw.ocs.client.messages.SequenceComponentMsg.{GetStatus, LoadScript, UnloadScript}

import scala.concurrent.Future

// fixme: can this take AkkaLocation similar to other wrappers like CommandService?
class SequenceComponentImpl(sequenceComponentRef: ActorRef[SequenceComponentMsg])(
    implicit scheduler: Scheduler,
    timeout: Timeout
) extends SequenceComponentApi {
  def loadScript(sequencerId: String, observingMode: String): Future[LoadScriptResponse] =
    sequenceComponentRef ? (LoadScript(sequencerId, observingMode, _))

  def getStatus: Future[GetStatusResponse] = sequenceComponentRef ? GetStatus

  def unloadScript(): Future[Done.type] = sequenceComponentRef ? UnloadScript
}
