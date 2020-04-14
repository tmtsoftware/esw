package esw.ocs.impl.core

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import esw.commons.Timeouts
import esw.ocs.api.models.Step
import esw.ocs.api.protocol.{OkOrUnhandledResponse, PullNextResponse}
import esw.ocs.api.actor.messages.SequencerMessages._

import scala.concurrent.Future

private[ocs] class SequenceOperator(sequencer: ActorRef[EswSequencerMessage])(implicit system: ActorSystem[_]) {
  private implicit val timeout: Timeout = Timeouts.LongTimeout

  def pullNext: Future[PullNextResponse]                = sequencer ? PullNext
  def maybeNext: Future[Option[Step]]                   = sequencer ? MaybeNext
  def readyToExecuteNext: Future[OkOrUnhandledResponse] = sequencer ? ReadyToExecuteNext
  def stepSuccess(): Unit                               = sequencer ! StepSuccess(system.deadLetters)
  def stepFailure(message: String): Unit                = sequencer ! StepFailure(message, system.deadLetters)
}
