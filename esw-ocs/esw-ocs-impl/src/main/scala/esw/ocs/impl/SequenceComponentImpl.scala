package esw.ocs.impl

import akka.Done
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.protocol.{GetStatusResponse, ScriptResponse}
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.messages.SequenceComponentMsg.{GetStatus, LoadScript, Restart, UnloadScript}

import scala.concurrent.Future

// fixme: can this take AkkaLocation similar to other wrappers like CommandService?
class SequenceComponentImpl(sequenceComponentRef: ActorRef[SequenceComponentMsg])(
    implicit actorSystem: ActorSystem[_],
    timeout: Timeout
) extends SequenceComponentApi {
  override def loadScript(packageId: String, observingMode: String): Future[ScriptResponse] =
    sequenceComponentRef ? (LoadScript(packageId, observingMode, _))

  override def restart(): Future[ScriptResponse] = sequenceComponentRef ? Restart

  override def status: Future[GetStatusResponse] = sequenceComponentRef ? GetStatus

  override def unloadScript(): Future[Done] = sequenceComponentRef ? UnloadScript
}
