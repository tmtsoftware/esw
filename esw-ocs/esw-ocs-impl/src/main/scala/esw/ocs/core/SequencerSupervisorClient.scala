package esw.ocs.core

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import esw.ocs.api.SequencerSupervisor
import esw.ocs.api.models.messages.SequencerMessages.{Abort, EswSequencerMessage, Shutdown}
import esw.ocs.api.models.messages.{AbortResponse, ShutdownResponse}

import scala.concurrent.Future

class SequencerSupervisorClient(sequencer: ActorRef[EswSequencerMessage])(
    implicit system: ActorSystem[_],
    timeout: Timeout
) extends SequencerSupervisor {

  private implicit val scheduler: Scheduler = system.scheduler

  override def shutdown(): Future[ShutdownResponse] = sequencer.ask(r => Shutdown(Some(r)))
  override def abort(): Future[AbortResponse]       = sequencer.ask(r => Abort(Some(r)))

}
