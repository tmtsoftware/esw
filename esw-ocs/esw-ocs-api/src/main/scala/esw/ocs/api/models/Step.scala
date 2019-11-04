package esw.ocs.api.models

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.models.StepStatus.{Finished, InFlight, Pending}
import esw.ocs.api.protocol.EditorError._

case class Step private[ocs] (id: Id, command: SequenceCommand, status: StepStatus, hasBreakpoint: Boolean) {
  def isPending: Boolean = status == StepStatus.Pending
  def isFailed: Boolean = status match {
    case _: Finished.Failure => true
    case _                   => false
  }
  def isFinished: Boolean = status match {
    case _: Finished => true
    case _           => false
  }

  def isInFlight: Boolean = status == StepStatus.InFlight

  def addBreakpoint(): Either[CannotOperateOnAnInFlightOrFinishedStep.type, Step] =
    if (isPending) Right(copy(hasBreakpoint = true))
    else Left(CannotOperateOnAnInFlightOrFinishedStep)

  def removeBreakpoint(): Step = if (hasBreakpoint) copy(hasBreakpoint = false) else this

  def withStatus(newStatus: StepStatus): Step =
    (status, newStatus) match {
      case (Pending, InFlight) | (InFlight, _: Finished) => copy(status = newStatus)
      case _                                             => this
    }

  private[ocs] def withId(id: Id): Step = copy(id = id)
}

object Step {
  def apply(command: SequenceCommand): Step = Step(Id(), command, StepStatus.Pending, hasBreakpoint = false)
  def apply(command: SequenceCommand, status: StepStatus, hasBreakpoint: Boolean): Step =
    new Step(Id(), command, status, hasBreakpoint)
}
