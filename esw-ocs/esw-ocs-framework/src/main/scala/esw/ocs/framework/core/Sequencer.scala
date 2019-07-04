package esw.ocs.framework.core
import akka.Done
import csw.params.commands.{CommandResponse, SequenceCommand}
import csw.params.core.models.Id
import esw.ocs.framework.api.models.messages.SequencerMsg._
import esw.ocs.framework.api.models.messages.StepListError._
import esw.ocs.framework.api.models.{Sequence, Step, StepList}

import scala.concurrent.Future

trait Sequencer {

  def processSequence(sequence: Sequence): Future[Either[ProcessSequenceError, CommandResponse.SubmitResponse]]

  def mayBeNext: Future[Option[Step]]
  def pullNext(): Future[Step]
  def getSequence: Future[StepList]
  def readyToExecuteNext(): Future[Done]

  // editor APIs
  def add(commands: List[SequenceCommand]): Future[Either[AddError, StepList]]

  def pause: Future[Either[PauseError, StepList]]

  def resume: Future[Either[ResumeError, StepList]]

  def discardPending: Future[Either[DiscardPendingError, StepList]]

  def replace(id: Id, commands: List[SequenceCommand]): Future[Either[ReplaceError, StepList]]

  def prepend(commands: List[SequenceCommand]): Future[Either[PrependError, StepList]]

  def delete(id: Id): Future[Either[DeleteError, StepList]]

  def addBreakpoint(id: Id): Future[Either[AddBreakpointError, StepList]]

  def removeBreakpoint(id: Id): Future[Either[RemoveBreakpointError, StepList]]

  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[Either[InsertError, StepList]]
}
