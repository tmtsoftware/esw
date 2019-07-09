package esw.ocs.framework.api.models

import akka.Done
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.framework.api.models.SequenceEditor.EditorResponse
import esw.ocs.framework.api.models.messages.StepListError._

import scala.concurrent.Future

trait SequenceEditor {
  def status: Future[StepList]
  def isAvailable: Future[Boolean]

  def add(commands: List[SequenceCommand]): Future[EditorResponse[AddError]]
  def prepend(commands: List[SequenceCommand]): Future[EditorResponse[PrependError]]
  def replace(id: Id, commands: List[SequenceCommand]): Future[EditorResponse[ReplaceError]]
  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[EditorResponse[InsertError]]
  def delete(id: Id): Future[EditorResponse[DeleteError]]
  def pause: Future[EditorResponse[PauseError]]
  def resume: Future[EditorResponse[ResumeError]]
  def addBreakpoint(id: Id): Future[EditorResponse[AddBreakpointError]]
  def removeBreakpoint(id: Id): Future[EditorResponse[RemoveBreakpointError]]
  def reset(): Future[EditorResponse[ResetError]]

  def shutdown(): Future[Unit]
  def abort(): Future[Unit]
}

object SequenceEditor {
  type EditorResponse[E] = Either[E, Done]
}
