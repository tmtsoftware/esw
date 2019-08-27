package esw.ocs.api.models

import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import esw.ocs.api.codecs.OcsAkkaSerializable
import esw.ocs.api.models.responses.EditorError._
import esw.ocs.api.models.responses.{DuplicateIdsFound, EditorError}

final case class StepList private[models] (runId: Id, steps: List[Step]) extends OcsAkkaSerializable {
  //query
  private[ocs] def isEmpty: Boolean    = steps.isEmpty
  def isFinished: Boolean              = !isEmpty && (steps.forall(_.isFinished) || steps.exists(_.isFailed))
  def isPaused: Boolean                = nextPending.exists(_.hasBreakpoint)
  def isInFlight: Boolean              = steps.exists(_.isInFlight)
  def isRunningButNotInFlight: Boolean = !isFinished && !isPaused && !isInFlight

  def nextPending: Option[Step]    = steps.find(_.isPending)
  def nextExecutable: Option[Step] = if (!isPaused) nextPending else None

  private def toSteps(commands: List[SequenceCommand]): List[Step] = commands.map(Step.apply)

  //update
  def replace(id: Id, commands: List[SequenceCommand]): Either[EditorError, StepList] =
    ifExists(id) { step =>
      if (step.isPending) replaceSteps(id, toSteps(commands))
      else Left(CannotOperateOnAnInFlightOrFinishedStep)
    }

  def prepend(commands: List[SequenceCommand]): StepList = {
    val (pre, post) = steps.span(!_.isPending)
    copy(runId, pre ::: toSteps(commands) ::: post)
  }

  // fixme: should check if given commands have duplicateIds
  def append(commands: List[SequenceCommand]): StepList = copy(runId, steps ::: toSteps(commands))

  def delete(id: Id): Either[EditorError, StepList] = ifExists(id) { _ =>
    steps
      .foldLeft[Either[EditorError, List[Step]]](Right(List.empty)) {
        case (acc, step) if step.id == id && step.isPending => acc
        case (_, step) if step.id == id                     => Left(CannotOperateOnAnInFlightOrFinishedStep)
        case (acc, step)                                    => acc.map(_ :+ step)
      }
      .map(steps => copy(runId, steps))
  }

  def insertAfter(id: Id, commands: List[SequenceCommand]): Either[EditorError, StepList] =
    ifExists[EditorError](id) { _ =>
      insertStepsAfter(id, toSteps(commands)).map(updatedSteps => copy(runId, updatedSteps))
    }

  def discardPending: StepList = copy(runId, steps.filterNot(_.isPending))

  def addBreakpoint(id: Id): Either[EditorError, StepList] = ifExists(id) { _ =>
    steps
      .foldLeft[Either[EditorError, List[Step]]](Right(List.empty)) {
        case (acc, step) if step.id == id => step.addBreakpoint().flatMap(step => acc.map(_ :+ step))
        case (acc, step)                  => acc.map(_ :+ step)
      }
      .map(steps => copy(runId, steps))
  }

  def removeBreakpoint(id: Id): Either[IdDoesNotExist, StepList] =
    ifExists(id)(_ => Right(updateAll(id, _.removeBreakpoint())))

  def pause: Either[CannotOperateOnAnInFlightOrFinishedStep.type, StepList] =
    nextPending
      .map(_.addBreakpoint().map(updateStep))
      .getOrElse(Left(CannotOperateOnAnInFlightOrFinishedStep))

  def resume: StepList =
    nextPending
      .map(step => updateStep(step.removeBreakpoint()))
      .getOrElse(this)

  private[ocs] def updateStatus(id: Id, stepStatus: StepStatus): StepList =
    copy(
      steps = steps
        .foldLeft[List[Step]](List.empty) {
          case (acc, step) if step.id == id => acc :+ step.withStatus(stepStatus)
          case (acc, step)                  => acc :+ step
        }
    )

  private def replaceSteps(id: Id, steps: List[Step]): Either[EditorError, StepList] =
    insertStepsAfter(id, steps).map(updatedSteps => copy(runId, updatedSteps.filterNot(_.id == id)))

  private def insertStepsAfter(id: Id, newSteps: List[Step]): Either[CannotOperateOnAnInFlightOrFinishedStep.type, List[Step]] = {
    val (pre, post)        = steps.span(_.id != id)
    val stepToInsertBefore = post.tail.headOption
    stepToInsertBefore match {
      case Some(step) if !step.isPending => Left(CannotOperateOnAnInFlightOrFinishedStep)
      case _                             => Right(pre ::: post.headOption.toList ::: newSteps ::: post.tail)
    }
  }

  private[ocs] def updateStep(step: Step): StepList = updateAll(step.id, _ => step)

  private def updateAll(id: Id, f: Step => Step): StepList =
    copy(runId, steps.map {
      case step if id == step.id => f(step)
      case step                  => step
    })

  private def ifExists[T <: EditorError](id: Id)(
      f: Step => Either[T, StepList]
  )(implicit ev: IdDoesNotExist <:< T): Either[T, StepList] =
    steps.find(_.id == id) match {
      case Some(step) => f(step)
      case None       => Left(IdDoesNotExist(id))
    }
}

object StepList {
  def apply(sequence: Sequence): Either[DuplicateIdsFound.type, StepList] = {
    val steps = sequence.commands.toList.map(Step.apply)
    if (steps.map(_.id).toSet.size == steps.size) Right(StepList(sequence.runId, steps))
    else Left(DuplicateIdsFound)
  }
}
