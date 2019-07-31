package esw.ocs.core

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.SequenceEditor
import esw.ocs.api.models.StepList
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages.SequencerResponses.EditorResponse

import scala.concurrent.Future

class SequenceEditorClient(sequencer: ActorRef[ExternalEditorMsg])(implicit system: ActorSystem[_], timeout: Timeout)
    extends SequenceEditor {
  private implicit val scheduler: Scheduler = system.scheduler

  override def status: Future[StepList]     = sequencer ? GetSequence
  override def isAvailable: Future[Boolean] = sequencer ? Available

  override def add(commands: List[SequenceCommand]): Future[EditorResponse] = sequencer ? (Add(commands, _))

  override def prepend(commands: List[SequenceCommand]): Future[EditorResponse] =
    sequencer ? (Prepend(commands, _))
  override def replace(id: Id, commands: List[SequenceCommand]): Future[EditorResponse] =
    sequencer ? (Replace(id, commands, _))
  override def insertAfter(id: Id, commands: List[SequenceCommand]): Future[EditorResponse] =
    sequencer ? (InsertAfter(id, commands, _))
  override def delete(id: Id): Future[EditorResponse]           = sequencer ? (Delete(id, _))
  override def pause: Future[EditorResponse]                    = sequencer ? Pause
  override def resume: Future[EditorResponse]                   = sequencer ? Resume
  override def addBreakpoint(id: Id): Future[EditorResponse]    = sequencer ? (AddBreakpoint(id, _))
  override def removeBreakpoint(id: Id): Future[EditorResponse] = sequencer ? (RemoveBreakpoint(id, _))
  override def reset(): Future[EditorResponse]                  = sequencer ? Reset
}
