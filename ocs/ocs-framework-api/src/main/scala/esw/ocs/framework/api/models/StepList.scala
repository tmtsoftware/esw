package esw.ocs.framework.api.models

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id

case class StepList(runId: Id, steps: List[Step]) { outer =>

  require(steps.map(_.id).toSet.size == steps.size, "steps can not have duplicate ids")
  
  //query

  def nextPending: Option[Step] = steps.find(_.isPending)
  def isPaused: Boolean         = nextPending.exists(_.hasBreakpoint)
  def next: Option[Step]        = if (!isPaused) nextPending else None
  def isFinished: Boolean       = steps.forall(_.isFinished)
  def isInFlight: Boolean       = steps.exists(_.isInFlight)

  //update

  def replace(id: Id, commands: List[SequenceCommand]): StepList = replaceSteps(id, Step.from(commands))
  private def replaceSteps(id: Id, steps: List[Step]): StepList  = insertStepsAfter(id, steps).delete(Set(id))

  def prepend(commands: List[SequenceCommand]): StepList = {
    val (pre, post) = steps.span(!_.isPending)
    copy(runId, pre ::: Step.from(commands) ::: post)
  }
  def append(commands: List[SequenceCommand]): StepList = copy(runId, steps ::: Step.from(commands))

  def delete(ids: Set[Id]): StepList = copy(runId, steps.filterNot(step => ids.contains(step.id) && step.isPending))

  def insertAfter(id: Id, commands: List[SequenceCommand]): StepList = insertStepsAfter(id, Step.from(commands))

  private def insertStepsAfter(id: Id, newSteps: List[Step]): StepList = {
    val (pre, post) = steps.span(_.id != id)
    copy(runId, pre ::: post.headOption.toList ::: newSteps ::: post.tail)
  }

  def discardPending: StepList = copy(runId, steps.filterNot(_.isPending))

  def addBreakpoints(ids: List[Id]): StepList    = updateAll(ids.toSet, _.addBreakpoint())
  def removeBreakpoints(ids: List[Id]): StepList = updateAll(ids.toSet, _.removeBreakpoint())

  def pause: StepList  = nextPending.map(step => updateStep(step.addBreakpoint())).flat
  def resume: StepList = nextPending.map(step => updateStep(step.removeBreakpoint())).flat

  def updateStep(step: Step): StepList                             = updateAll(Set(step.id), _ => step)
  def updateStatus(ids: Set[Id], stepStatus: StepStatus): StepList = updateAll(ids, _.withStatus(stepStatus))

  def updateAll(ids: Set[Id], f: Step => Step): StepList =
    copy(runId, {
      steps.map {
        case step if ids.contains(step.id) => f(step)
        case step                          => step
      }
    })

  private implicit class StepOps(optStep: Option[StepList]) {
    def flat: StepList = optStep.getOrElse(outer)
  }
}

object StepList {
  def empty: StepList                    = StepList(Id(), List.empty)
  def from(sequence: Sequence): StepList = StepList(sequence.runId, Step.from(sequence.commands.toList))
}
