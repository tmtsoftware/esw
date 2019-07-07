package esw.ocs.framework.api.models

import akka.Done
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.framework.api.models.messages.StepListError._

import scala.concurrent.Future

trait SequenceEditor {
  def add(commands: List[SequenceCommand]): Future[Either[AddError, Done]]
  def pause: Future[Either[PauseError, Done]]
  def resume: Future[Either[ResumeError, Done]]
  def reset(): Future[Either[ResetError, Done]]
  def status: Future[StepList]
  def isAvailable: Future[Boolean]
  def delete(id: Id): Future[Either[DeleteError, Done]]
  def addBreakpoint(id: Id): Future[Either[AddBreakpointError, Done]]
  def removeBreakpoint(id: Id): Future[Either[RemoveBreakpointError, Done]]
  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[Either[InsertError, Done]]
  def prepend(commands: List[SequenceCommand]): Future[Either[PrependError, Done]]
  def replace(id: Id, commands: List[SequenceCommand]): Future[Either[ReplaceError, Done]]
  def shutdown(): Future[Unit]
  def abort(): Future[Unit]
}
