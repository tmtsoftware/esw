package esw.ocs.framework.api.models

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.framework.api.models.StepStatus.{Finished, InFlight, Pending}

case class StepResponse(isSuccessful: Boolean, step: Step)

case class Step(command: SequenceCommand, status: StepStatus, hasBreakpoint: Boolean) {
  def id: Id              = command.runId
  def isPending: Boolean  = status == StepStatus.Pending
  def isFinished: Boolean = status == StepStatus.Finished
  def isInFlight: Boolean = status == StepStatus.InFlight

  def addBreakpoint(): StepResponse =
    if (isPending) StepResponse(isSuccessful = true, copy(hasBreakpoint = true))
    else StepResponse(isSuccessful = false, this)

  def removeBreakpoint(): Step = if (hasBreakpoint) copy(hasBreakpoint = false) else this

  def withStatus(newStatus: StepStatus): StepResponse =
    (status, newStatus) match {
      case (Pending, InFlight)  => StepResponse(isSuccessful = false, copy(status = newStatus))
      case (InFlight, Finished) => StepResponse(isSuccessful = true, copy(status = newStatus))
      case _                    => StepResponse(isSuccessful = true, this)
    }
}

object Step {
  def from(command: SequenceCommand): Step              = Step(command, StepStatus.Pending, hasBreakpoint = false)
  def from(commands: List[SequenceCommand]): List[Step] = commands.map(command => from(command))
}
