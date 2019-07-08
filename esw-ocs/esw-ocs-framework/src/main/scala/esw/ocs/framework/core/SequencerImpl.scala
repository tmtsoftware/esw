package esw.ocs.framework.core

import akka.Done
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.params.commands.CommandResponse.{Completed, Error, Started, SubmitResponse}
import csw.params.commands.{CommandResponse, SequenceCommand}
import csw.params.core.models.Id
import esw.ocs.async.macros.StrandEc
import esw.ocs.framework.api.models.StepStatus.{Finished, InFlight, Pending}
import esw.ocs.framework.api.models._
import esw.ocs.framework.api.models.messages.ProcessSequenceError.ExistingSequenceIsInProcess
import esw.ocs.framework.api.models.messages.StepListError._
import esw.ocs.framework.api.models.messages.{ProcessSequenceError, StepListError}
import esw.ocs.framework.dsl.Async.{async, await}
import esw.ocs.framework.syntax.EitherSyntax._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

private[framework] class SequencerImpl(crm: CommandResponseManager)(implicit strandEc: StrandEc, timeout: Timeout) {
  private implicit val singleThreadedEc: ExecutionContext = strandEc.ec
  private val emptyChildId                                = Id("empty-child")

  private var stepList                                         = StepList.empty
  private var previousStepList: Option[StepList]               = None
  private var readyToExecuteNextPromise: Option[Promise[Done]] = None
  private var stepRefPromise: Option[Promise[Step]]            = None
  private var sequencerAvailable                               = true

  def processSequence(sequence: Sequence): Future[Either[ProcessSequenceError, SubmitResponse]] =
    async {
      if (sequencerAvailable)
        await(
          StepList(sequence)
            .traverse { _stepList ⇒
              sequencerAvailable = false
              updateStepList(_stepList)
              val id = _stepList.runId
              crm.addOrUpdateCommand(Started(id))
              crm.addSubCommand(id, emptyChildId)
              completeReadyToExecuteNextPromise()
              handleSequenceResponse(crm.queryFinal(id))
            }
        )
      else Left(ExistingSequenceIsInProcess)
    }

  def pullNext(): Future[Step] = async {
    stepList.nextExecutable match {
      // step.isPending check is actually not required, but kept here in case impl of nextExecutable gets changed
      case Some(step) if step.isPending ⇒ setPendingToInFlight(step)
      case None                         ⇒ await(createStepRefPromise())
    }
  }

  def readyToExecuteNext(): Future[Done] = async {
    val notInFlight = !stepList.isInFlight
    val notFinished = !stepList.isFinished

    if (notInFlight && notFinished) Done
    else await(createReadyToExecuteNextPromise())
  }

  def isAvailable: Future[Boolean]                                          = async(sequencerAvailable)
  def getSequence: Future[StepList]                                         = async(stepList)
  def mayBeNext: Future[Option[Step]]                                       = async(stepList.nextExecutable)
  def add(commands: List[SequenceCommand]): Future[Either[AddError, Done]]  = updateStepListResult(stepList.append(commands))
  def pause: Future[Either[PauseError, Done]]                               = updateStepListResult(stepList.pause)
  def resume: Future[Either[ResumeError, Done]]                             = updateStepListResult(stepList.resume)
  def reset(): Future[Either[ResetError, Done]]                             = updateStepListResult(stepList.discardPending)
  def delete(id: Id): Future[Either[DeleteError, Done]]                     = updateStepListResult(stepList.delete(id))
  def addBreakpoint(id: Id): Future[Either[AddBreakpointError, Done]]       = updateStepListResult(stepList.addBreakpoint(id))
  def removeBreakpoint(id: Id): Future[Either[RemoveBreakpointError, Done]] = updateStepListResult(stepList.removeBreakpoint(id))
  def replace(id: Id, commands: List[SequenceCommand]): Future[Either[ReplaceError, Done]] =
    updateStepListResult(stepList.replace(id, commands))
  def prepend(commands: List[SequenceCommand]): Future[Either[PrependError, Done]] =
    updateStepListResult(stepList.prepend(commands))
  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[Either[InsertError, Done]] =
    updateStepListResult(stepList.insertAfter(id, commands))

  private def updateStepList(newStepList: StepList): Unit = {
    if (!stepList.isEmpty)
      previousStepList = Some(stepList)
    stepList = newStepList
  }

  private def handleSequenceResponse(submitResponse: Future[SubmitResponse]) = {
    submitResponse.onComplete(_ ⇒ resetState())
    submitResponse.map(CommandResponse.withRunId(stepList.runId, _))
  }

  // this method gets called from places where it is already checked that step is in pending status
  private def setPendingToInFlight(step: Step) = {
    val inflightStep = step.withStatus(Pending, InFlight)
    stepList = stepList.updateStep(inflightStep)
    val stepRunId = step.id
    crm.addSubCommand(stepList.runId, stepRunId)
    crm.addOrUpdateCommand(CommandResponse.Started(stepRunId))
    processSubmitResponse(stepRunId, crm.queryFinal(stepRunId))
    inflightStep
  }

  private def processSubmitResponse(stepId: Id, submitResponseF: Future[SubmitResponse]) =
    submitResponseF
      .flatMap {
        case submitResponse if CommandResponse.isPositive(submitResponse) ⇒
          updateStatus(submitResponse, Finished.Success(submitResponse))
        case submitResponse ⇒ updateStatus(submitResponse, Finished.Failure(submitResponse))
      }
      .recoverWith {
        case NonFatal(e) ⇒
          val errorResponse = Error(stepId, e.getMessage)
          updateStatus(errorResponse, Finished.Failure(errorResponse))
      }

  private def updateStatus(submitResponse: SubmitResponse, stepStatus: StepStatus) = async {
    crm.updateSubCommand(submitResponse)
    val updateStatusResult = stepList.updateStatus(submitResponse.runId, stepStatus)
    updateStatusResult.foreach { _stepList ⇒
      stepList = _stepList
      checkForSequenceCompletion()
      completeReadyToExecuteNextPromise()
    }
    updateStatusResult
  }

  private def checkForSequenceCompletion(): Unit = if (stepList.isFinished) {
    crm.updateSubCommand(Completed(emptyChildId))
  }

  private def resetState(): Unit = {
    stepRefPromise.foreach(_.complete(Failure(new RuntimeException("Trying to pull Step from a finished Sequence."))))
    stepRefPromise = None
    sequencerAvailable = true
  }

  private def createReadyToExecuteNextPromise() =
    createPromise[Done](p ⇒ readyToExecuteNextPromise = Some(p))

  private def completeReadyToExecuteNextPromise() = async {
    if (!stepList.isFinished)
      readyToExecuteNextPromise.foreach(_.complete(Success(Done)))
  }

  private def createStepRefPromise() = createPromise[Step](p ⇒ stepRefPromise = Some(p))

  private def completeStepRefPromise(): Unit =
    for {
      ref  <- stepRefPromise
      step <- stepList.nextExecutable
      if step.isPending
    } {
      ref.complete(Try(setPendingToInFlight(step)))
      stepRefPromise = None
    }

  private def createPromise[T](update: Promise[T] ⇒ Unit): Future[T] = async {
    val p = Promise[T]()
    update(p)
    await(p.future)
  }

  // stepListResultFunc is by name because all StepList operations must execute on strandEc
  private def updateStepListResult[T <: StepListError](stepListResultFunc: ⇒ Either[T, StepList]) = async {
    val stepListResult = stepListResultFunc
    stepListResult.map { s ⇒
      stepList = s
      checkForSequenceCompletion()
      completeStepRefPromise()
      Done
    }
  }
}
