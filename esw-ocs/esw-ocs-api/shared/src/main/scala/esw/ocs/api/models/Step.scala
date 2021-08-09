package esw.ocs.api.models

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.models.StepStatus.{Finished, InFlight, Pending}
import esw.ocs.api.protocol.EditorError.*

/**
 * This is a class which represent the Step model(a runtime representation of sequence command)
 *
 * @param id - an unique id for the step
 * @param command - original sequence command
 * @param status - status of step
 * @param hasBreakpoint - represents if there is a breakpoint on the step
 */
case class Step private[ocs] (id: Id, command: SequenceCommand, status: StepStatus, hasBreakpoint: Boolean) {
  def isPending: Boolean = status == StepStatus.Pending
  def isFailed: Boolean =
    status match {
      case _: Finished.Failure => true
      case _                   => false
    }
  def isFinished: Boolean =
    status match {
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

  private[ocs] def info = s"StepId: ${id.id}, CommandName: ${command.commandName.name}"
}

object Step {
  def apply(command: SequenceCommand): Step = Step(Id(), command, StepStatus.Pending, hasBreakpoint = false)
  def apply(command: SequenceCommand, status: StepStatus, hasBreakpoint: Boolean): Step =
    new Step(Id(), command, status, hasBreakpoint)
}
