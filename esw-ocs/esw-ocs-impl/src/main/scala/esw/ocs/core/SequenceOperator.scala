package esw.ocs.core

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.params.commands.CommandResponse.SubmitResponse
import esw.ocs.api.models.Step
import esw.ocs.core.messages.SequencerMessages._
import esw.ocs.api.models.responses.{OkOrUnhandledResponse, PullNextResponse}
import esw.ocs.internal.Timeouts

import scala.concurrent.Future

private[ocs] class SequenceOperator(sequencer: ActorRef[EswSequencerMessage])(implicit system: ActorSystem[_]) {
  private implicit val scheduler: Scheduler = system.scheduler
  private implicit val timeout: Timeout     = Timeouts.LongTimeout

  def pullNext: Future[PullNextResponse]                = sequencer ? PullNext
  def maybeNext: Future[Option[Step]]                   = sequencer ? MaybeNext
  def readyToExecuteNext: Future[OkOrUnhandledResponse] = sequencer ? ReadyToExecuteNext
  def update(submitResponse: SubmitResponse): Unit      = sequencer ! Update(submitResponse, system.deadLetters)
}
