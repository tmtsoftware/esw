package esw.ocs.impl.core

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.params.commands.CommandResponse.SubmitResponse
import esw.ocs.api.models.Step
import esw.ocs.api.protocol.{OkOrUnhandledResponse, PullNextResponse}
import esw.ocs.dsl.script.SequenceOperator
import esw.ocs.impl.internal.Timeouts
import esw.ocs.impl.messages.SequencerMessages._

import scala.concurrent.Future

private[ocs] class SequenceOperatorImpl(sequencer: ActorRef[EswSequencerMessage])(implicit system: ActorSystem[_])
    extends SequenceOperator {
  private implicit val timeout: Timeout = Timeouts.LongTimeout

  def pullNext: Future[PullNextResponse]                = sequencer ? PullNext
  def maybeNext: Future[Option[Step]]                   = sequencer ? MaybeNext
  def readyToExecuteNext: Future[OkOrUnhandledResponse] = sequencer ? ReadyToExecuteNext
  def update(submitResponse: SubmitResponse): Unit      = sequencer ! Update(submitResponse, system.deadLetters)
}
