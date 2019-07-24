package esw.ocs.core

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import esw.ocs.api.SequencerSupervisor
import esw.ocs.api.models.messages.LifecycleResponse
import esw.ocs.api.models.messages.SequencerMessages.{Abort, LifecycleMsg, Shutdown}

import scala.concurrent.Future

class SequencerSupervisorClient(sequencer: ActorRef[LifecycleMsg])(
    implicit system: ActorSystem[_],
    timeout: Timeout
) extends SequencerSupervisor {

  private implicit val scheduler: Scheduler = system.scheduler

// It is Ok to call Try.get inside future
  override def shutdown(): Future[LifecycleResponse] = sequencer ? Shutdown
  override def abort(): Future[LifecycleResponse]    = sequencer ? Abort

}
