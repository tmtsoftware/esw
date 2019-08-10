package esw.ocs.api

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.models.messages._

import scala.concurrent.Future

trait SequenceEditor {

  def status: Future[GetSequenceResponse]
  def add(commands: List[SequenceCommand]): Future[SimpleResponse]
  def prepend(commands: List[SequenceCommand]): Future[SimpleResponse]
  def replace(id: Id, commands: List[SequenceCommand]): Future[SimpleResponse]
  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[SimpleResponse]
  def delete(id: Id): Future[SimpleResponse]
  def pause: Future[SimpleResponse]
  def resume: Future[SimpleResponse]
  def addBreakpoint(id: Id): Future[SimpleResponse]
  def removeBreakpoint(id: Id): Future[SimpleResponse]
  def reset(): Future[SimpleResponse]
}
