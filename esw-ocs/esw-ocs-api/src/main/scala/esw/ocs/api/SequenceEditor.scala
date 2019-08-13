package esw.ocs.api

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.models.messages._

import scala.concurrent.Future

trait SequenceEditor {

  def getSequence: Future[GetSequenceResponse]
  def getPreviousSequence: Future[GetPreviousSequenceResponse]
  def add(commands: List[SequenceCommand]): Future[SimpleResponse]
  def prepend(commands: List[SequenceCommand]): Future[SimpleResponse]
  def replace(id: Id, commands: List[SequenceCommand]): Future[ComplexResponse]
  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[ComplexResponse]
  def delete(id: Id): Future[ComplexResponse]
  def pause: Future[PauseResponse]
  def resume: Future[SimpleResponse]
  def addBreakpoint(id: Id): Future[ComplexResponse]
  def removeBreakpoint(id: Id): Future[RemoveBreakpointResponse]
  def reset(): Future[SimpleResponse]
}
