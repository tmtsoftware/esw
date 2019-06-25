package esw.ocs.framework.api.models

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id

final case class StepListResponse(reply: StepListActionResponse, stepList: StepList)

final case class StepList private (runId: Id, steps: List[Step]) { outer =>
  //query
  def nextPending: Option[Step] = steps.find(_.isPending)
  def isPaused: Boolean         = nextPending.exists(_.hasBreakpoint)
  def next: Option[Step]        = if (!isPaused) nextPending else None
  def isFinished: Boolean       = steps.forall(_.isFinished)
  def isInFlight: Boolean       = steps.exists(_.isInFlight)

  def checkIfFinished(f: ⇒ StepListResponse): StepListResponse =
    if (isFinished) StepListResponse(NotAllowedOnFinishedSeq, this) else f

  def checkIfExists(id: Id)(f: ⇒ StepListResponse): StepListResponse =
    steps.find(_.id == id) match {
      case None ⇒ StepListResponse(NotAllowedOnFinishedSeq, this)
      case _    ⇒ f
    }

  //update
  def replace(id: Id, commands: List[SequenceCommand]): StepListResponse =
    checkIfExists(id)(checkIfFinished(replaceSteps(id, Step.from(commands))))

  private def replaceSteps(id: Id, steps: List[Step]): StepListResponse = insertStepsAfter(id, steps).stepList.delete(Set(id))

  def prepend(commands: List[SequenceCommand]): StepListResponse = checkIfFinished {
    val (pre, post) = steps.span(!_.isPending)
    StepListResponse(Completed, copy(runId, pre ::: Step.from(commands) ::: post))
  }

  def append(commands: List[SequenceCommand]): StepListResponse =
    checkIfFinished(StepListResponse(Completed, copy(runId, steps ::: Step.from(commands))))

  def delete(ids: Set[Id]): StepListResponse =
    checkIfFinished(StepListResponse(Completed, copy(runId, steps.filterNot(step => ids.contains(step.id) && step.isPending))))

  def insertAfter(id: Id, commands: List[SequenceCommand]): StepListResponse =
    checkIfExists(id)(checkIfFinished(insertStepsAfter(id, Step.from(commands))))

  private def insertStepsAfter(id: Id, newSteps: List[Step]): StepListResponse = {
    val (pre, post) = steps.span(_.id != id)
    StepListResponse(Completed, copy(runId, pre ::: post.headOption.toList ::: newSteps ::: post.tail))
  }

  def discardPending: StepListResponse = checkIfFinished(StepListResponse(Completed, copy(runId, steps.filterNot(_.isPending))))

  def addBreakpoints(ids: List[Id]): StepListResponse = checkIfFinished {
    var addedIds    = List.empty[Id]
    var notAddedIds = List.empty[Id]

    val updatedSteps = steps.map {
      case step if ids.contains(step.id) =>
        val StepResponse(isSuccessful, updatedStep) = step.addBreakpoint()
        if (isSuccessful) addedIds = updatedStep.id :: addedIds
        else notAddedIds = updatedStep.id :: notAddedIds
        updatedStep
      case step => step
    }

    notAddedIds ::= ids.toSet diff (addedIds ::: notAddedIds).toSet

    val updatedStepList = copy(runId, updatedSteps)
    StepListResponse(AdditionResult(addedIds, notAddedIds), updatedStepList)
  }

  def removeBreakpoints(ids: List[Id]): StepListResponse = checkIfFinished {
    StepListResponse(Completed, updateAll(ids.toSet, _.removeBreakpoint()))
  }

  def pause: StepListResponse =
    checkIfFinished {
      nextPending
        .map { step =>
          val StepResponse(isSuccessful, updatedStep) = step.addBreakpoint()
          if (isSuccessful) updateStep(Paused, updatedStep)
          else updateStep(PauseFailed, updatedStep)
        }
        .getOrElse(PauseFailed)
    }

  def resume: StepListResponse = checkIfFinished {
    nextPending.map(step => updateStep(step.removeBreakpoint())).getOrElse(Completed) // completed?
  }

  def updateStep(step: Step): StepListResponse = checkIfExists(step.id)(updateStep(Completed, step))

  private def updateStep[T <: StepListActionResponse](reply: T, step: Step): StepListResponse = checkIfFinished {
    StepListResponse(reply, updateAll(Set(step.id), _ => step))
  }

  // api changed from prototype (single Id instead of Set[Id]), confirm?
  def updateStatus(id: Id, stepStatus: StepStatus): StepListResponse =
    checkIfExists(id)(checkIfFinished {
      var reply: UpdateResponse = Updated

      // what if id not found?
      val updatedSteps = steps.map {
        case step if id == step.id =>
          val StepResponse(isSuccessful, updatedStep) = step.withStatus(stepStatus)
          if (isSuccessful) reply = Updated
          else reply = UpdateFailed
          updatedStep
        case step => step
      }
      val updatedStepList = copy(runId, updatedSteps)
      StepListResponse(reply, updatedStepList)
    })

  private def updateAll(ids: Set[Id], f: Step => Step): StepList =
    copy(runId, {
      steps.map {
        case step if ids.contains(step.id) => f(step)
        case step                          => step
      }
    })

  private implicit class StepOps(optStep: Option[StepListResponse]) {
    def getOrElse[T <: StepListActionResponse](reply: T): StepListResponse = optStep.getOrElse(StepListResponse(reply, outer))
  }
}

object StepList {
  case object DuplicateIdsFound

  def empty: StepList = StepList(Id(), List.empty)

  def from(sequence: Sequence): Either[DuplicateIdsFound.type, StepList] = {
    val steps = Step.from(sequence.commands.toList)

    if (steps.map(_.id).toSet.size == steps.size) Right(StepList(sequence.runId, steps))
    else Left(DuplicateIdsFound)
  }
}
