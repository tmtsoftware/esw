package esw.ocs.framework.core

import akka.Done
import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.framework.api.models.messages.SequencerMsg._
import esw.ocs.framework.api.models.messages.StepListError._
import esw.ocs.framework.api.models.{SequenceEditor, StepList}

import scala.concurrent.Future

class SequenceEditorClient(sequencer: ActorRef[ExternalSequencerMsg])(implicit system: ActorSystem[_], timeout: Timeout)
    extends SequenceEditor {
  private implicit val scheduler: Scheduler = system.scheduler
  import system.executionContext

  override def add(commands: List[SequenceCommand]): Future[Either[AddError, Done]]  = sequencer ? (Add(commands, _))
  override def pause: Future[Either[PauseError, Done]]                               = sequencer ? Pause
  override def resume: Future[Either[ResumeError, Done]]                             = sequencer ? Resume
  override def reset(): Future[Either[ResetError, Done]]                             = sequencer ? Reset
  override def status: Future[StepList]                                              = sequencer ? GetSequence
  override def isAvailable: Future[Boolean]                                          = sequencer ? Available
  override def delete(id: Id): Future[Either[DeleteError, Done]]                     = sequencer ? (Delete(id, _))
  override def addBreakpoint(id: Id): Future[Either[AddBreakpointError, Done]]       = sequencer ? (AddBreakpoint(id, _))
  override def removeBreakpoint(id: Id): Future[Either[RemoveBreakpointError, Done]] = sequencer ? (RemoveBreakpoint(id, _))
  override def insertAfter(id: Id, commands: List[SequenceCommand]): Future[Either[InsertError, Done]] =
    sequencer ? (InsertAfter(id, commands, _))

  override def prepend(commands: List[SequenceCommand]): Future[Either[PrependError, Done]] = sequencer ? (Prepend(commands, _))
  override def replace(id: Id, commands: List[SequenceCommand]): Future[Either[ReplaceError, Done]] =
    sequencer ? (Replace(id, commands, _))

  // It is Ok to call Try.get inside future
  override def shutdown(): Future[Unit] = (sequencer ? Shutdown).map(_.get)
  override def abort(): Future[Unit]    = (sequencer ? Abort).map(_.get)
}
