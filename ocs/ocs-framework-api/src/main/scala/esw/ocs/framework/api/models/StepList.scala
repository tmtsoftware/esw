package esw.ocs.framework.api.models

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.framework.api.models.messages.StepListActionResponse._
import esw.ocs.framework.api.models.messages._

final case class StepListResult[T <: StepListActionResponse](response: T, stepList: StepList)

final case class StepList private[models] (runId: Id, steps: List[Step]) { outer =>
  //query
  // todo: what should we return when StepList is empty?
  def isFinished: Boolean = steps.forall(_.isFinished)
  def isPaused: Boolean   = nextPending.exists(_.hasBreakpoint)
  def isInFlight: Boolean = steps.exists(_.isInFlight)

  def nextPending: Option[Step]    = steps.find(_.isPending)
  def nextExecutable: Option[Step] = if (!isPaused) nextPending else None

  private def toSteps(commands: List[SequenceCommand]): List[Step] = commands.map(Step.apply)

  //update
  def replace(id: Id, commands: List[SequenceCommand]): StepListResult[ReplaceResponse] =
    checkIfExists(id)(checkIfFinished(replaceSteps(id, toSteps(commands))))

  private def replaceSteps(id: Id, steps: List[Step]): StepListResult[ReplaceResponse] =
    insertStepsAfter(id, steps, Replaced).stepList.filterNot(Set(id), Replaced)

  def prepend(commands: List[SequenceCommand]): StepListResult[PrependResponse] = checkIfFinished {
    val (pre, post) = steps.span(!_.isPending)
    StepListResult(Prepended, copy(runId, pre ::: toSteps(commands) ::: post))
  }

  def append(commands: List[SequenceCommand]): StepListResult[AddResponse] =
    checkIfFinished(StepListResult(Added, copy(runId, steps ::: toSteps(commands))))

  def delete(ids: Set[Id]): StepListResult[DeleteResponse] = checkIfFinished(filterNot(ids, Deleted))

  def insertAfter(id: Id, commands: List[SequenceCommand]): StepListResult[InsertAfterResponse] =
    checkIfExists(id)(checkIfFinished(insertStepsAfter(id, toSteps(commands), Inserted)))

  def discardPending: StepListResult[DiscardPendingResponse] =
    checkIfFinished(StepListResult(Discarded, copy(runId, steps.filterNot(_.isPending))))

  def addBreakpoints(ids: List[Id]): StepListResult[AddBreakpointsResponse] = checkIfFinished {
    var addedIds    = List.empty[Id]
    var notAddedIds = List.empty[Id]

    val updatedSteps = steps.map {
      case step if ids.contains(step.id) =>
        val StepResult(isSuccessful, updatedStep) = step.addBreakpoint()
        if (isSuccessful) addedIds = updatedStep.id :: addedIds
        else notAddedIds = updatedStep.id :: notAddedIds
        updatedStep
      case step => step
    }

    notAddedIds = notAddedIds ::: (ids.toSet diff (addedIds ::: notAddedIds).toSet).toList

    val updatedStepList = copy(runId, updatedSteps)
    StepListResult(AdditionResult(addedIds, notAddedIds), updatedStepList)
  }

  def removeBreakpoints(ids: List[Id]): StepListResult[RemoveBreakpointsResponse] = checkIfFinished {
    StepListResult(BreakpointsRemoved, updateAll(ids.toSet, _.removeBreakpoint()))
  }

  def pause: StepListResult[PauseResponse] =
    checkIfFinished {
      nextPending
        .map { step =>
          val StepResult(isSuccessful, updatedStep) = step.addBreakpoint()
          if (isSuccessful) updateStep[PauseResponse](updatedStep, Paused)
          else updateStep[PauseResponse](updatedStep, PauseFailed)
        }
        .getOrReturn(PauseFailed)
    }

  def resume: StepListResult[ResumeResponse] = checkIfFinished {
    nextPending.map(step => updateStep[ResumeResponse](step.removeBreakpoint(), Resumed)).getOrReturn(Resumed) // completed?
  }

  def updateStep(step: Step): StepListResult[UpdateResponse] = checkIfExists(step.id)(updateStep(step, Updated))

  // api changed from prototype (single Id instead of Set[Id]), confirm?
  def updateStatus(id: Id, stepStatus: StepStatus): StepListResult[UpdateResponse] =
    checkIfExists(id)(checkIfFinished {
      var reply: UpdateResponse = Updated

      val updatedSteps = steps.map {
        case step if id == step.id =>
          val StepResult(isSuccessful, updatedStep) = step.withStatus(stepStatus)
          if (isSuccessful) reply = Updated
          else reply = UpdateFailed
          updatedStep
        case step => step
      }
      val updatedStepList = copy(runId, updatedSteps)
      StepListResult(reply, updatedStepList)
    })

  private def insertStepsAfter[T <: StepListActionResponse](
      id: Id,
      newSteps: List[Step],
      response: T
  ): StepListResult[T] = {
    val (pre, post) = steps.span(_.id != id)
    StepListResult(response, copy(runId, pre ::: post.headOption.toList ::: newSteps ::: post.tail))
  }

  private def updateStep[T <: StepListActionResponse](step: Step, response: T): StepListResult[T] =
    checkIfFinished {
      StepListResult(response, updateAll(Set(step.id), _ => step))
    }

  private def filterNot[T <: StepListActionResponse](ids: Set[Id], response: T): StepListResult[T] =
    StepListResult(response, copy(runId, steps.filterNot(step => ids.contains(step.id) && step.isPending)))

  private def updateAll(ids: Set[Id], f: Step => Step): StepList =
    copy(runId, steps.map {
      case step if ids.contains(step.id) => f(step)
      case step                          => step
    })

  private def checkIfFinished[T <: StepListActionResponse](f: ⇒ StepListResult[T]): StepListResult[T] =
    if (isFinished) StepListResult(NotAllowedOnFinishedSeq.asInstanceOf[T], this) else f

  private def checkIfExists[T <: StepListActionResponse](id: Id)(f: ⇒ StepListResult[T]): StepListResult[T] =
    steps.find(_.id == id) match {
      case None ⇒ StepListResult(IdDoesNotExist(id).asInstanceOf[T], this)
      case _    ⇒ f
    }

  private implicit class StepListResultOps[T <: StepListActionResponse](optStep: Option[StepListResult[T]]) {
    def getOrReturn(response: T): StepListResult[T] = optStep.getOrElse(StepListResult(response, outer))
  }
}

object StepList {
  case object DuplicateIdsFound

  def empty: StepList = StepList(Id(), List.empty)

  def apply(sequence: Sequence): Either[DuplicateIdsFound.type, StepList] = {
    val steps = sequence.commands.toList.map(Step.apply)

    if (steps.map(_.id).toSet.size == steps.size) Right(StepList(sequence.runId, steps))
    else Left(DuplicateIdsFound)
  }

}
