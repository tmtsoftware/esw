package esw.ocs.framework.api.models

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.framework.api.models.messages.SequencerMsg.DuplicateIdsFound
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
    ifExistAndNotFinished(id) { step ⇒
      if (step.isPending) replaceSteps(id, toSteps(commands))
      else StepListResult(ReplaceNotSupportedInThisStatus(id, step.status), this)
    }

  def prepend(commands: List[SequenceCommand]): StepListResult[PrependResponse] = ifNotFinished {
    val (pre, post) = steps.span(!_.isPending)
    StepListResult(Prepended, copy(runId, pre ::: toSteps(commands) ::: post))
  }

  def append(commands: List[SequenceCommand]): StepListResult[AddResponse] =
    ifNotFinished(StepListResult(Added, copy(runId, steps ::: toSteps(commands))))

  def delete(ids: Set[Id]): StepListResult[DeleteResponse] = ifNotFinished {
    val successFailIds = new SuccessFailState(ids)

    val updatedSteps = steps.filterNot {
      case step if ids.contains(step.id) && step.isPending ⇒ successFailIds.addSuccess(step.id); true
      case step if ids.contains(step.id)                   ⇒ successFailIds.addFailure(step.id); false
      case _                                               ⇒ false
    }

    StepListResult(DeletionResult(successFailIds.successIds, successFailIds.failureIds), copy(runId, updatedSteps))
  }

  def insertAfter(id: Id, commands: List[SequenceCommand]): StepListResult[InsertAfterResponse] =
    ifExistAndNotFinished(id) { _ ⇒
      val updatedSteps = insertStepsAfter(id, toSteps(commands))
      StepListResult(Inserted, copy(runId, updatedSteps))
    }

  def discardPending: StepListResult[DiscardPendingResponse] =
    ifNotFinished(StepListResult(Discarded, copy(runId, steps.filterNot(_.isPending))))

  def addBreakpoints(ids: List[Id]): StepListResult[AddBreakpointsResponse] = ifNotFinished {
    val successFailIds = new SuccessFailState(ids.toSet)

    val updatedSteps = steps.map {
      case step if ids.contains(step.id) =>
        val StepResult(isSuccessful, updatedStep) = step.addBreakpoint()
        if (isSuccessful) successFailIds.addSuccess(updatedStep.id)
        else successFailIds.addFailure(updatedStep.id)
        updatedStep
      case step => step
    }
    StepListResult(AdditionResult(successFailIds.successIds, successFailIds.failureIds), copy(runId, updatedSteps))
  }

  def removeBreakpoints(ids: List[Id]): StepListResult[RemoveBreakpointsResponse] = ifNotFinished {
    StepListResult(BreakpointsRemoved, updateAll(ids.toSet, _.removeBreakpoint()))
  }

  def pause: StepListResult[PauseResponse] =
    ifNotFinished {
      nextPending
        .map { step =>
          val StepResult(isSuccessful, updatedStep) = step.addBreakpoint()
          if (isSuccessful) StepListResult[PauseResponse](Paused, updateStep(updatedStep))
          else StepListResult[PauseResponse](PauseFailed, updateStep(updatedStep))
        }
        .getOrReturn(PauseFailed)
    }

  def resume: StepListResult[ResumeResponse] = ifNotFinished {
    nextPending
      .map(step => StepListResult[ResumeResponse](Resumed, updateStep(step.removeBreakpoint())))
      .getOrReturn(Resumed)
  }

  // api changed from prototype (single Id instead of Set[Id]), confirm?
  private[framework] def updateStatus(id: Id, stepStatus: StepStatus): StepListResult[UpdateResponse] =
    ifExistAndNotFinished(id) { _ ⇒
      var reply: UpdateResponse = UpdateFailed

      val updatedSteps = steps.map {
        case step if id == step.id =>
          val stepResult = step.withStatus(stepStatus)
          if (stepResult.isSuccessful) reply = Updated(stepResult.step)
          else reply = UpdateFailed
          stepResult.step
        case step => step
      }

      val updatedStepList = copy(runId, updatedSteps)
      StepListResult(reply, updatedStepList)
    }

  private def replaceSteps(id: Id, steps: List[Step]): StepListResult[ReplaceResponse] =
    StepListResult(Replaced, copy(runId, insertStepsAfter(id, steps).filterNot(_.id == id)))

  private def insertStepsAfter(id: Id, newSteps: List[Step]): List[Step] = {
    val (pre, post) = steps.span(_.id != id)
    pre ::: post.headOption.toList ::: newSteps ::: post.tail
  }

  private def updateStep(step: Step) = updateAll(Set(step.id), _ => step)

  private def updateAll(ids: Set[Id], f: Step => Step): StepList =
    copy(runId, steps.map {
      case step if ids.contains(step.id) => f(step)
      case step                          => step
    })

  private def ifNotFinished[T <: StepListActionResponse](f: ⇒ StepListResult[T]): StepListResult[T] =
    if (isFinished) StepListResult(NotAllowedOnFinishedSeq.asInstanceOf[T], this) else f

  private def ifExists[T <: StepListActionResponse](id: Id)(f: Step ⇒ StepListResult[T]): StepListResult[T] =
    steps.find(_.id == id) match {
      case Some(step) ⇒ f(step)
      case None       ⇒ StepListResult(IdDoesNotExist(id).asInstanceOf[T], this)
    }

  private def ifExistAndNotFinished[T <: StepListActionResponse](id: Id)(f: Step ⇒ StepListResult[T]): StepListResult[T] =
    ifExists(id)(step ⇒ ifNotFinished(f(step)))

  private implicit class StepListResultOps[T <: StepListActionResponse](optStep: Option[StepListResult[T]]) {
    def getOrReturn(response: T): StepListResult[T] = optStep.getOrElse(StepListResult(response, outer))
  }

  private class SuccessFailState(allIds: Set[Id]) {
    private var _successIds = List.empty[Id]
    private var _failureIds = List.empty[Id]

    def addFailure(id: Id): Unit = _failureIds ::= id
    def addSuccess(id: Id): Unit = _successIds ::= id

    def successIds: List[Id] = _successIds

    // add not processed ids into failure ids
    def failureIds: List[Id] = _failureIds ::: (allIds diff (_successIds ::: _failureIds).toSet).toList
  }
}

object StepList {

  def empty: StepList = StepList(Id(), List.empty)

  def apply(sequence: Sequence): Either[DuplicateIdsFound.type, StepList] = {
    val steps = sequence.commands.toList.map(Step.apply)

    if (steps.map(_.id).toSet.size == steps.size) Right(StepList(sequence.runId, steps))
    else Left(DuplicateIdsFound)
  }

}
