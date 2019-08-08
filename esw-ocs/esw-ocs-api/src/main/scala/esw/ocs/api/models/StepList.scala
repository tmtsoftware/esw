package esw.ocs.api.models

import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import esw.ocs.api.models.messages.EditorError
import esw.ocs.api.models.messages.EditorError._
import esw.ocs.api.models.messages.SequenceError.DuplicateIdsFound
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

final case class StepList private[models] (runId: Id, steps: List[Step]) extends OcsFrameworkAkkaSerializable {
  //query
  private[ocs] def isEmpty: Boolean = steps.isEmpty
  def isFinished: Boolean           = !isEmpty && (steps.forall(_.isFinished) || steps.exists(_.isFailed))
  def isPaused: Boolean             = nextPending.exists(_.hasBreakpoint)
  def isInFlight: Boolean           = steps.exists(_.isInFlight)

  def nextPending: Option[Step]    = steps.find(_.isPending)
  def nextExecutable: Option[Step] = if (!isPaused) nextPending else None

  private def toSteps(commands: List[SequenceCommand]): List[Step] = commands.map(Step.apply)

  //update
  def replace(id: Id, commands: List[SequenceCommand]): Either[ReplaceError, StepList] =
    ifExists(id) { step =>
      if (step.isPending) replaceSteps(id, toSteps(commands))
      else Left(NotSupported(step.status))
    }

  def prepend(commands: List[SequenceCommand]): Either[EditorError, StepList] = {
    val (pre, post) = steps.span(!_.isPending)
    Right(copy(runId, pre ::: toSteps(commands) ::: post))
  }

  def append(commands: List[SequenceCommand]): Either[EditorError, StepList] =
    Right(copy(runId, steps ::: toSteps(commands)))

  def delete(id: Id): Either[DeleteError, StepList] = ifExists(id) { _ =>
    steps
      .foldLeft[Either[DeleteError, List[Step]]](Right(List.empty)) {
        case (acc, step) if step.id == id && step.isPending => acc
        case (_, step) if step.id == id                     => Left(NotSupported(step.status))
        case (acc, step)                                    => acc.map(_ :+ step)
      }
      .map(steps => copy(runId, steps))
  }

  def insertAfter(id: Id, commands: List[SequenceCommand]): Either[InsertError, StepList] =
    ifExists[InsertError](id) { _ =>
      insertStepsAfter(id, toSteps(commands)).map(updatedSteps => copy(runId, updatedSteps))
    }

  def discardPending: Either[EditorError, StepList] = Right(copy(runId, steps.filterNot(_.isPending)))

  def addBreakpoint(id: Id): Either[AddBreakpointError, StepList] = ifExists(id) { _ =>
    steps
      .foldLeft[Either[AddBreakpointError, List[Step]]](Right(List.empty)) {
        case (acc, step) if step.id == id => step.addBreakpoint().flatMap(step => acc.map(_ :+ step))
        case (acc, step)                  => acc.map(_ :+ step)
      }
      .map(steps => copy(runId, steps))
  }

  def removeBreakpoint(id: Id): Either[RemoveBreakpointError, StepList] =
    ifExists(id)(_ => Right(updateAll(id, _.removeBreakpoint())))

  def pause: Either[PauseError, StepList] =
    nextPending
      .map(_.addBreakpoint().map(updateStep))
      .getOrElse(Left(PauseFailed("No pending step found, pausing is only supported for pending steps")))

  def resume: Either[EditorError, StepList] =
    nextPending
      .map(step => Right(updateStep(step.removeBreakpoint())))
      .getOrElse(Right(this))

  private[ocs] def updateStatus(id: Id, stepStatus: StepStatus): Either[UpdateError, StepList] =
    ifExists(id) { _ =>
      steps
        .foldLeft[Either[UpdateError, List[Step]]](Right(List.empty)) {
          case (acc, step) if step.id == id => step.withStatus(stepStatus).flatMap(step => acc.map(_ :+ step))
          case (acc, step)                  => acc.map(_ :+ step)
        }
        .map(steps => copy(runId, steps))
    }

  private def replaceSteps(id: Id, steps: List[Step]): Either[ReplaceError, StepList] =
    insertStepsAfter(id, steps).map(updatedSteps => copy(runId, updatedSteps.filterNot(_.id == id)))

  private def insertStepsAfter(id: Id, newSteps: List[Step]): Either[NotSupported, List[Step]] = {
    val (pre, post)        = steps.span(_.id != id)
    val stepToInsertBefore = post.tail.headOption
    stepToInsertBefore match {
      case Some(step) if !step.isPending => Left(NotSupported(step.status))
      case _                             => Right(pre ::: post.headOption.toList ::: newSteps ::: post.tail)
    }
  }

  private[ocs] def updateStep(step: Step) = updateAll(step.id, _ => step)

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

  def empty: StepList = StepList(Id(), List.empty)

  def apply(sequence: Sequence): Either[DuplicateIdsFound.type, StepList] = {
    val steps = sequence.commands.toList.map(Step.apply)
    if (steps.map(_.id).toSet.size == steps.size) Right(StepList(sequence.runId, steps))
    else Left(DuplicateIdsFound)
  }
}
