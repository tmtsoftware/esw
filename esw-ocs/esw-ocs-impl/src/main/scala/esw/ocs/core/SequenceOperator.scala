package esw.ocs.core

import akka.Done
import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.params.commands.CommandResponse.SubmitResponse
import esw.ocs.api.models.Step
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.internal.Timeouts

import scala.concurrent.Future

private[ocs] class SequenceOperator(sequencer: ActorRef[InternalSequencerMsg])(implicit system: ActorSystem[_]) {
  private implicit val scheduler: Scheduler = system.scheduler
  private implicit val timeout: Timeout     = Timeouts.EngineTimeout

  def pullNext: Future[Step]                       = sequencer ? PullNext
  def maybeNext: Future[Option[Step]]              = sequencer ? MaybeNext
  def readyToExecuteNext: Future[Done]             = sequencer ? ReadyToExecuteNext
  def update(submitResponse: SubmitResponse): Unit = sequencer ! UpdateFailure(submitResponse)
}
