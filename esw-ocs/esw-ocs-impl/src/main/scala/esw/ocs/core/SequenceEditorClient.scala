package esw.ocs.core

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.SequenceEditor
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages._

import scala.concurrent.Future

class SequenceEditorClient(sequencer: ActorRef[EswSequencerMessage])(implicit system: ActorSystem[_], timeout: Timeout)
    extends SequenceEditor {
  private implicit val scheduler: Scheduler = system.scheduler

  override def status: Future[GetSequenceResponse] = sequencer.ask(r => GetSequence(Some(r)))

  override def add(commands: List[SequenceCommand]): Future[SimpleResponse] = sequencer.ask(r => Add(commands, Some(r)))

  override def prepend(commands: List[SequenceCommand]): Future[SimpleResponse] =
    sequencer.ask(r => Prepend(commands, Some(r)))

  override def replace(id: Id, commands: List[SequenceCommand]): Future[SimpleResponse] =
    sequencer.ask(r => Replace(id, commands, Some(r)))

  override def insertAfter(id: Id, commands: List[SequenceCommand]): Future[SimpleResponse] =
    sequencer.ask(r => InsertAfter(id, commands, Some(r)))

  override def delete(id: Id): Future[SimpleResponse] = sequencer.ask(r => Delete(id, Some(r)))

  override def pause: Future[SimpleResponse] = sequencer.ask(r => Pause(Some(r)))

  override def resume: Future[SimpleResponse] = sequencer.ask(r => Resume(Some(r)))

  override def addBreakpoint(id: Id): Future[SimpleResponse] =
    sequencer.ask(r => AddBreakpoint(id, Some(r)))

  override def removeBreakpoint(id: Id): Future[SimpleResponse] =
    sequencer.ask(r => RemoveBreakpoint(id, Some(r)))

  override def reset(): Future[SimpleResponse] = sequencer.ask(r => Reset(Some(r)))
}
