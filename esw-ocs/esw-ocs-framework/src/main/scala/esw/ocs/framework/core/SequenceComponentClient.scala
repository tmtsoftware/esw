package esw.ocs.framework.core

import akka.Done
import akka.actor.Scheduler
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.location.model.scaladsl.AkkaLocation
import esw.ocs.framework.api.models.messages.SequenceComponentMsg
import esw.ocs.framework.api.models.messages.SequenceComponentMsg.{GetStatus, LoadScript, UnloadScript}

import scala.concurrent.Future
import scala.util.Try

class SequenceComponentClient(sequenceComponentRef: ActorRef[SequenceComponentMsg])(
    implicit scheduler: Scheduler,
    timeout: Timeout
) {
  def loadScript(
      sequencerId: String,
      observingMode: String
  ): Future[Try[AkkaLocation]] = sequenceComponentRef ? (x => LoadScript(sequencerId, observingMode, x))

  def getStatus: Future[Option[AkkaLocation]] = sequenceComponentRef ? GetStatus

  def unloadScript(): Future[Done] = sequenceComponentRef ? UnloadScript
}
