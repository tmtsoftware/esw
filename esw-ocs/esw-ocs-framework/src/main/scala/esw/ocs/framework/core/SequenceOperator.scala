package esw.ocs.framework.core

import akka.Done
import akka.actor.Scheduler
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.params.commands.CommandResponse.SubmitResponse
import esw.ocs.framework.api.models.Step
import esw.ocs.framework.api.models.messages.SequencerMsg._

import scala.concurrent.Future

class SequenceOperator(sequencer: ActorRef[InternalSequencerMsg])(
    implicit system: ActorSystem[_],
    timeout: Timeout
) {
  private implicit val scheduler: Scheduler = system.scheduler

  def pullNext: Future[Step]                       = sequencer ? PullNext
  def maybeNext: Future[Option[Step]]              = sequencer ? MaybeNext
  def readyToExecuteNext: Future[Done]             = sequencer ? ReadyToExecuteNext
  def update(submitResponse: SubmitResponse): Unit = sequencer ! UpdateFailure(submitResponse)
}
