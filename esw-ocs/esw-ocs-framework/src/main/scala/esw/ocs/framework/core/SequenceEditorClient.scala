package esw.ocs.framework.core

import akka.Done
import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.framework.api.models.SequenceEditor.EditorResponse
import esw.ocs.framework.api.models.messages.SequencerMsg._
import esw.ocs.framework.api.models.messages.error.StepListError._
import esw.ocs.framework.api.models.messages.error.{SequencerAbortError, SequencerShutdownError}
import esw.ocs.framework.api.models.{SequenceEditor, StepList}

import scala.concurrent.Future

class SequenceEditorClient(sequencer: ActorRef[ExternalSequencerMsg])(implicit system: ActorSystem[_], timeout: Timeout)
    extends SequenceEditor {
  private implicit val scheduler: Scheduler = system.scheduler

  override def status: Future[StepList]     = sequencer ? GetSequence
  override def isAvailable: Future[Boolean] = sequencer ? Available

  override def add(commands: List[SequenceCommand]): Future[EditorResponse[AddError]]         = sequencer ? (Add(commands, _))
  override def prepend(commands: List[SequenceCommand]): Future[EditorResponse[PrependError]] = sequencer ? (Prepend(commands, _))
  override def replace(id: Id, commands: List[SequenceCommand]): Future[EditorResponse[ReplaceError]] =
    sequencer ? (Replace(id, commands, _))
  override def insertAfter(id: Id, commands: List[SequenceCommand]): Future[EditorResponse[InsertError]] =
    sequencer ? (InsertAfter(id, commands, _))
  override def delete(id: Id): Future[EditorResponse[DeleteError]]                     = sequencer ? (Delete(id, _))
  override def pause: Future[EditorResponse[PauseError]]                               = sequencer ? Pause
  override def resume: Future[EditorResponse[ResumeError]]                             = sequencer ? Resume
  override def addBreakpoint(id: Id): Future[EditorResponse[AddBreakpointError]]       = sequencer ? (AddBreakpoint(id, _))
  override def removeBreakpoint(id: Id): Future[EditorResponse[RemoveBreakpointError]] = sequencer ? (RemoveBreakpoint(id, _))
  override def reset(): Future[EditorResponse[ResetError]]                             = sequencer ? Reset

  // It is Ok to call Try.get inside future
  override def shutdown(): Future[Either[SequencerShutdownError, Done]] = sequencer ? Shutdown
  override def abort(): Future[Either[SequencerAbortError, Done]]       = sequencer ? Abort
}
