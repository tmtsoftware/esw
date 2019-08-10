package esw.ocs.api

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.models.messages._

import scala.concurrent.Future

trait SequenceEditor {

  def status: Future[GetSequenceResponse]
  def add(commands: List[SequenceCommand]): Future[AddResponse]
  def prepend(commands: List[SequenceCommand]): Future[PrependResponse]
  def replace(id: Id, commands: List[SequenceCommand]): Future[ReplaceResponse]
  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[InsertAfterResponse]
  def delete(id: Id): Future[DeleteResponse]
  def pause: Future[PauseResponse]
  def resume: Future[ResumeResponse]
  def addBreakpoint(id: Id): Future[AddBreakpointResponse]
  def removeBreakpoint(id: Id): Future[RemoveBreakpointResponse]
  def reset(): Future[ResetResponse]

}
