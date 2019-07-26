package esw.ocs.core

import akka.Done
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.command.client.messages.sequencer.SequenceError.ExistingSequenceIsInProcess
import csw.command.client.messages.sequencer.SequenceResponse
import csw.params.commands.CommandResponse.{Completed, Error, Started, SubmitResponse}
import csw.params.commands.{CommandResponse, Sequence, SequenceCommand}
import csw.params.core.models.Id
import esw.ocs.api.models.StepStatus._
import esw.ocs.api.models.messages.LoadSequenceResponse
import esw.ocs.api.models.messages.error.StepListError
import esw.ocs.api.models.messages.error.StepListError._
import esw.ocs.api.models.{Step, StepList, StepStatus}
import esw.ocs.dsl.Async.{async, await}
import esw.ocs.macros.StrandEc

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

private[ocs] class Sequencer(crm: CommandResponseManager)(implicit strandEc: StrandEc, timeout: Timeout) {
  private implicit val singleThreadedEc: ExecutionContext = strandEc.ec
  private val emptyChildId                                = Id("empty-child") // fixme

  private var stepList                                         = StepList.empty
  private var loadedStepList                                   = StepList.empty
  private var previousStepList: Option[StepList]               = None
  private var readyToExecuteNextPromise: Option[Promise[Done]] = None
  private var stepRefPromise: Option[Promise[Step]]            = None
  private var sequencerAvailable                               = true

  def load(sequence: Sequence): Future[LoadSequenceResponse] = async {
    LoadSequenceResponse(
      if (sequencerAvailable) {
        StepList(sequence).map { _stepList =>
          loadedStepList = _stepList
          Done
        }
      } else Left(ExistingSequenceIsInProcess)
    )
  }

  def start(): Future[SequenceResponse] = async {
    SequenceResponse(
      if (sequencerAvailable) {
        sequencerAvailable = false
        updateStepList(loadedStepList)
        val id = stepList.runId
        crm.addOrUpdateCommand(Started(id))
        crm.addSubCommand(id, emptyChildId)
        completeStepRefPromise()
        completeReadyToExecuteNextPromise() // To complete the promise created for previous sequence so that engine can pullNext
        await(handleSequenceResponse(crm.queryFinal(id)).map(Right(_)))
      } else Left(ExistingSequenceIsInProcess)
    )
  }

  def loadAndStart(sequence: Sequence): Future[SequenceResponse] =
    load(sequence).flatMap(_ => start())

  def pullNext(): Future[Step] = async {
    stepList.nextExecutable match {
      // step.isPending check is actually not required, but kept here in case impl of nextExecutable gets changed
      case Some(step) if step.isPending => setPendingToInFlight(step)
      case None                         => await(createStepRefPromise())
    }
  }

  def readyToExecuteNext(): Future[Done] = async {
    if (stepList.isInFlight || stepList.isFinished) await(createReadyToExecuteNextPromise())
    else Done
  }

  def isAvailable: Future[Boolean] = async(sequencerAvailable)

  def getSequence: Future[StepList] = async(stepList)

  def getPreviousSequence: Future[Option[StepList]] = async(previousStepList)

  def mayBeNext: Future[Option[Step]] = async(stepList.nextExecutable)

  // editor
  def add(commands: List[SequenceCommand]): Future[Either[AddError, Done]] = updateStepListResult(stepList.append(commands))

  def prepend(commands: List[SequenceCommand]): Future[Either[PrependError, Done]] =
    updateStepListResult(stepList.prepend(commands))

  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[Either[InsertError, Done]] =
    updateStepListResult(stepList.insertAfter(id, commands))

  def replace(id: Id, commands: List[SequenceCommand]): Future[Either[ReplaceError, Done]] =
    updateStepListResult(stepList.replace(id, commands))

  def delete(id: Id): Future[Either[DeleteError, Done]] = updateStepListResult(stepList.delete(id))

  def pause: Future[Either[PauseError, Done]] = updateStepListResult(stepList.pause)

  def resume: Future[Either[ResumeError, Done]] = updateStepListResult(stepList.resume)

  def reset(): Future[Either[ResetError, Done]] = updateStepListResult(stepList.discardPending)

  def addBreakpoint(id: Id): Future[Either[AddBreakpointError, Done]] = updateStepListResult(stepList.addBreakpoint(id))

  def removeBreakpoint(id: Id): Future[Either[RemoveBreakpointError, Done]] =
    updateStepListResult(stepList.removeBreakpoint(id))

  def shutdown(): Unit = strandEc.shutdown()

  private def updateStepList(newStepList: StepList): Unit = {
    if (!stepList.isEmpty)
      previousStepList = Some(stepList)
    stepList = newStepList
  }

  private def handleSequenceResponse(submitResponse: Future[SubmitResponse]): Future[SubmitResponse] = {
    submitResponse.onComplete(_ => resetState())
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
        case submitResponse if CommandResponse.isPositive(submitResponse) => updateSuccess(submitResponse)
        case failureResponse                                              => updateFailure(failureResponse)
      }
      .recoverWith {
        case NonFatal(e) => updateFailure(Error(stepId, e.getMessage))
      }

  private[ocs] def updateFailure(failureResponse: SubmitResponse) =
    updateStatus(failureResponse, Finished.Failure(failureResponse))

  private def updateSuccess(successResponse: SubmitResponse) = updateStatus(successResponse, Finished.Success(successResponse))

  private def updateStatus(submitResponse: SubmitResponse, stepStatus: StepStatus) = async {
    crm.updateSubCommand(submitResponse)
    val updateStatusResult = stepList.updateStatus(submitResponse.runId, stepStatus)
    updateStatusResult.foreach { _stepList =>
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
    createPromise[Done](p => readyToExecuteNextPromise = Some(p))

  private def completeReadyToExecuteNextPromise() = async {
    if (!stepList.isFinished)
      readyToExecuteNextPromise.foreach(_.complete(Success(Done)))
  }

  private def createStepRefPromise() = createPromise[Step](p => stepRefPromise = Some(p))

  private def completeStepRefPromise(): Unit =
    for {
      ref  <- stepRefPromise
      step <- stepList.nextExecutable
      if step.isPending
    } {
      ref.complete(Try(setPendingToInFlight(step)))
      stepRefPromise = None
    }

  private def createPromise[T](update: Promise[T] => Unit): Future[T] = async {
    val p = Promise[T]()
    update(p)
    await(p.future)
  }

  // stepListResultFunc is by name because all StepList operations must execute on strandEc
  private def updateStepListResult[T <: StepListError](stepListResultFunc: => Either[T, StepList]) = async {
    val stepListResult = stepListResultFunc
    stepListResult.map { s =>
      stepList = s
      checkForSequenceCompletion()
      completeStepRefPromise()
      Done
    }
  }
}
