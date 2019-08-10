package esw.ocs.core

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.params.commands.CommandResponse.SubmitResponse
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages.{MaybeNextResponse, PullNextResponse, SimpleResponse}
import esw.ocs.internal.Timeouts

import scala.concurrent.Future

private[ocs] class SequenceOperator(sequencer: ActorRef[EswSequencerMessage])(implicit system: ActorSystem[_]) {
  private implicit val scheduler: Scheduler = system.scheduler
  private implicit val timeout: Timeout     = Timeouts.EngineTimeout

  def pullNext: Future[PullNextResponse]           = sequencer.ask(r => PullNext(Some(r)))
  def maybeNext: Future[MaybeNextResponse]         = sequencer.ask(r => MaybeNext(Some(r)))
  def readyToExecuteNext: Future[SimpleResponse]   = sequencer.ask(r => ReadyToExecuteNext(Some(r)))
  def update(submitResponse: SubmitResponse): Unit = sequencer ! UpdateFailure(submitResponse, None)
}
