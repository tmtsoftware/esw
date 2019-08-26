package esw.ocs.api

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.models.StepList
import esw.ocs.api.models.responses._

import scala.concurrent.Future

trait SequencerAdminApi {

  def getSequence: Future[Option[StepList]]
//  def getState: Future[SequencerState[SequencerMsg]] fixme: Extract cross compilable APIs which give idea of if sequence available or offline online etc
  def add(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse]
  def prepend(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse]
  def replace(id: Id, commands: List[SequenceCommand]): Future[GenericResponse]
  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[GenericResponse]
  def delete(id: Id): Future[GenericResponse]
  def pause: Future[PauseResponse]
  def resume: Future[OkOrUnhandledResponse]
  def addBreakpoint(id: Id): Future[GenericResponse]
  def removeBreakpoint(id: Id): Future[RemoveBreakpointResponse]
  def reset(): Future[OkOrUnhandledResponse]
  def abortSequence(): Future[OkOrUnhandledResponse]
  def goOnline(): Future[GoOnlineResponse]
  def goOffline(): Future[OkOrUnhandledResponse]

}
