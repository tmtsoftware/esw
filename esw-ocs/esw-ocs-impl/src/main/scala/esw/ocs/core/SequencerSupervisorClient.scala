package esw.ocs.core

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import esw.ocs.api.SequencerSupervisor
import esw.ocs.api.models.messages.SequencerMessages.{Abort, EswSequencerMessage, Shutdown}
import esw.ocs.api.models.messages.SimpleResponse

import scala.concurrent.Future

class SequencerSupervisorClient(sequencer: ActorRef[EswSequencerMessage])(
    implicit system: ActorSystem[_],
    timeout: Timeout
) extends SequencerSupervisor {

  private implicit val scheduler: Scheduler = system.scheduler

  override def shutdown(): Future[SimpleResponse] = sequencer.ask(r => Shutdown(Some(r)))
  override def abort(): Future[SimpleResponse]    = sequencer.ask(r => Abort(Some(r)))

}
