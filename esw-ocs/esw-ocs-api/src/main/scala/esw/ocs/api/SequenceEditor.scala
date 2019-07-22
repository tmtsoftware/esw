package esw.ocs.api

import akka.Done
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.models.StepList
import esw.ocs.api.models.messages.error.EditorError

import scala.concurrent.Future

case class EditorResponse(response: Either[EditorError, Done])

trait SequenceEditor {

  def status: Future[StepList]
  def isAvailable: Future[Boolean]

  def add(commands: List[SequenceCommand]): Future[EditorResponse]
  def prepend(commands: List[SequenceCommand]): Future[EditorResponse]
  def replace(id: Id, commands: List[SequenceCommand]): Future[EditorResponse]
  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[EditorResponse]
  def delete(id: Id): Future[EditorResponse]
  def pause: Future[EditorResponse]
  def resume: Future[EditorResponse]
  def addBreakpoint(id: Id): Future[EditorResponse]
  def removeBreakpoint(id: Id): Future[EditorResponse]
  def reset(): Future[EditorResponse]

  def shutdown(): Future[EditorResponse]
  def abort(): Future[EditorResponse]
}
