package esw.ocs.framework.api.models

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.framework.api.models.StepStatus.{Finished, InFlight, Pending}

case class StepResult(isSuccessful: Boolean, step: Step)

case class Step private[models] (command: SequenceCommand, status: StepStatus, hasBreakpoint: Boolean) {
  def id: Id              = command.runId
  def isPending: Boolean  = status == StepStatus.Pending
  def isFinished: Boolean = status == StepStatus.Finished
  def isInFlight: Boolean = status == StepStatus.InFlight

  def addBreakpoint(): StepResult =
    if (isPending) StepResult(isSuccessful = true, copy(hasBreakpoint = true))
    else StepResult(isSuccessful = false, this)

  def removeBreakpoint(): Step = if (hasBreakpoint) copy(hasBreakpoint = false) else this

  def withStatus(newStatus: StepStatus): StepResult =
    (status, newStatus) match {
      case (Pending, InFlight) | (InFlight, Finished) => StepResult(isSuccessful = true, copy(status = newStatus))
      case _                                          => StepResult(isSuccessful = false, this)
    }
}

object Step {
  def apply(command: SequenceCommand): Step = Step(command, StepStatus.Pending, hasBreakpoint = false)
}
