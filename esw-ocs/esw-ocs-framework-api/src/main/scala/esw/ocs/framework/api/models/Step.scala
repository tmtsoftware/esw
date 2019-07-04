package esw.ocs.framework.api.models

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.framework.api.models.StepStatus.{Finished, InFlight, Pending}
import esw.ocs.framework.api.models.messages.StepListError.{AddingBreakpointNotSupported, UpdateNotSupported}

case class StepResult(isSuccessful: Boolean, step: Step)

case class Step private[models] (command: SequenceCommand, status: StepStatus, hasBreakpoint: Boolean) {
  def id: Id             = command.runId
  def isPending: Boolean = status == StepStatus.Pending
  def isFinished: Boolean = status match {
    case _: Finished ⇒ true
    case _           ⇒ false
  }

  def isInFlight: Boolean = status == StepStatus.InFlight

  def addBreakpoint(): Either[AddingBreakpointNotSupported, Step] =
    if (isPending) Right(copy(hasBreakpoint = true))
    else Left(AddingBreakpointNotSupported(status))

  def removeBreakpoint(): Step = if (hasBreakpoint) copy(hasBreakpoint = false) else this

  def withStatus(newStatus: StepStatus): Either[UpdateNotSupported, Step] =
    (status, newStatus) match {
      case (Pending, InFlight) | (InFlight, _: Finished) => Right(copy(status = newStatus))
      case (from, to)                                    => Left(UpdateNotSupported(from, to))
    }
}

object Step {
  def apply(command: SequenceCommand): Step = Step(command, StepStatus.Pending, hasBreakpoint = false)
}
