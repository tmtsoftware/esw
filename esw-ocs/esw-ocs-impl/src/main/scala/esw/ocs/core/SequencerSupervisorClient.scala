package esw.ocs.core

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import esw.ocs.api.SequencerSupervisor
import esw.ocs.api.models.messages.OkOrUnhandledResponse
import esw.ocs.api.models.messages.SequencerMessages.{AbortSequence, EswSequencerMessage, Shutdown}

import scala.concurrent.Future

class SequencerSupervisorClient(sequencer: ActorRef[EswSequencerMessage])(
    implicit system: ActorSystem[_],
    timeout: Timeout
) extends SequencerSupervisor {

  private implicit val scheduler: Scheduler = system.scheduler

  override def shutdown(): Future[OkOrUnhandledResponse]      = sequencer ? Shutdown
  override def abortSequence(): Future[OkOrUnhandledResponse] = sequencer ? AbortSequence

}
