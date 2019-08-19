package esw.ocs.api

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.models.messages._

import scala.concurrent.Future

trait SequenceEditor {

  def getSequence: Future[StepListResponse]
  def getPreviousSequence: Future[StepListResponse]
  def add(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse]
  def prepend(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse]
  def replace(id: Id, commands: List[SequenceCommand]): Future[GenericResponse]
  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[GenericResponse]
  def delete(id: Id): Future[GenericResponse]
  def pause: Future[PauseResponse]
  def resume: Future[OkOrUnhandledResponse]
  def addBreakpoint(id: Id): Future[GenericResponse]
  def removeBreakpoint(id: Id): Future[RemoveBreakpointResponse]
}
