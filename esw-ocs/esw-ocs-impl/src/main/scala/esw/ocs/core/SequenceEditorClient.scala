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

  override def status: Future[GetSequenceResponse] = sequencer ? GetSequence

  override def add(commands: List[SequenceCommand]): Future[SimpleResponse] = sequencer.ask(r => Add(commands, r))

  override def prepend(commands: List[SequenceCommand]): Future[SimpleResponse] = sequencer ? (Prepend(commands, _))

  override def replace(id: Id, commands: List[SequenceCommand]): Future[SimpleResponse] = sequencer ? (Replace(id, commands, _))

  override def insertAfter(id: Id, commands: List[SequenceCommand]): Future[SimpleResponse] =
    sequencer ? (InsertAfter(id, commands, _))

  override def delete(id: Id): Future[SimpleResponse] = sequencer ? (Delete(id, _))

  override def pause: Future[SimpleResponse] = sequencer ? Pause

  override def resume: Future[SimpleResponse] = sequencer ? Resume

  override def addBreakpoint(id: Id): Future[SimpleResponse] = sequencer ? (AddBreakpoint(id, _))

  override def removeBreakpoint(id: Id): Future[SimpleResponse] = sequencer ? (RemoveBreakpoint(id, _))

  override def reset(): Future[SimpleResponse] = sequencer ? Reset
}
