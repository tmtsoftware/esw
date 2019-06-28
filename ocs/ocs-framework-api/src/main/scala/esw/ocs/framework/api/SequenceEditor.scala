package esw.ocs.framework.api

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.framework.api.models.StepList
import esw.ocs.framework.api.models.messages.StepListActionResponse._

import scala.concurrent.Future

trait SequenceEditor {
  def addAll(commands: List[SequenceCommand]): Future[List[AddResponse]]
  def pause(): Future[PauseResponse]
  def resume(): Future[ResumeResponse]
  // fixme: revisit type
  def reset(): Future[DiscardPendingResponse]
  def status: Future[StepList]
  def isAvailable: Future[Boolean]
  def delete(ids: List[Id]): Future[DeleteResponse]
  def addBreakpoints(ids: List[Id]): Future[AddBreakpointsResponse]
  def removeBreakpoints(ids: List[Id]): Future[RemoveBreakpointsResponse]
  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[InsertAfterResponse]
  def prepend(commands: List[SequenceCommand]): Future[PrependResponse]
  def replace(id: Id, commands: List[SequenceCommand]): Future[ReplaceResponse]
  def shutdown(): Future[Unit]
  def abort(): Future[Unit]
}
