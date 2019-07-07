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
import scala.util.{Success, Try}

private[framework] class SequencerImpl(crm: CommandResponseManager)(implicit strandEc: StrandEc, timeout: Timeout) {
  private implicit val singleThreadedEc: ExecutionContext = strandEc.ec
  private val emptyChildId                                = Id("empty-child")

  private var stepList                                         = StepList.empty
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
              val id = _stepList.runId
              stepList = _stepList
              crm.addOrUpdateCommand(Started(id))
              crm.addSubCommand(id, emptyChildId)
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

  def isAvailable: Future[Boolean]                                          = async(sequencerAvailable)
  def getSequence: Future[StepList]                                         = async(stepList)
  def mayBeNext: Future[Option[Step]]                                       = async(stepList.nextExecutable)
  def add(commands: List[SequenceCommand]): Future[Either[AddError, Done]]  = update(stepList.append(commands))
  def pause: Future[Either[PauseError, Done]]                               = update(stepList.pause)
  def resume: Future[Either[ResumeError, Done]]                             = update(stepList.resume)
  def reset(): Future[Either[ResetError, Done]]                             = update(stepList.discardPending)
  def delete(id: Id): Future[Either[DeleteError, Done]]                     = update(stepList.delete(id))
  def addBreakpoint(id: Id): Future[Either[AddBreakpointError, Done]]       = update(stepList.addBreakpoint(id))
  def removeBreakpoint(id: Id): Future[Either[RemoveBreakpointError, Done]] = update(stepList.removeBreakpoint(id))
  def replace(id: Id, commands: List[SequenceCommand]): Future[Either[ReplaceError, Done]] =
    update(stepList.replace(id, commands))
  def prepend(commands: List[SequenceCommand]): Future[Either[PrependError, Done]] = update(stepList.prepend(commands))
  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[Either[InsertError, Done]] =
    update(stepList.insertAfter(id, commands))

  def readyToExecuteNext(): Future[Done] = async {
    if (!stepList.isInFlight) Done
    else await(createReadyToExecuteNextPromise())
  }

  private def handleSequenceResponse(submitResponse: Future[SubmitResponse]) = {
    submitResponse.onComplete { _ ⇒
      // fixme: Confirm
      completeReadyToExecuteNextPromise()
      resetState()
    }
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
          update(submitResponse, Finished.Success(submitResponse))
        case submitResponse ⇒ update(submitResponse, Finished.Failure(submitResponse))
      }
      .recoverWith {
        case NonFatal(e) ⇒
          val errorResponse = Error(stepId, e.getMessage)
          update(errorResponse, Finished.Failure(errorResponse))
      }

  private def update(submitResponse: SubmitResponse, stepStatus: StepStatus) = async {
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
    stepList = StepList.empty
    readyToExecuteNextPromise = None
    sequencerAvailable = true
  }

  private def createReadyToExecuteNextPromise() =
    createPromise[Done](p ⇒ readyToExecuteNextPromise = Some(p))

  private def completeReadyToExecuteNextPromise() = async {
    readyToExecuteNextPromise.foreach(_.complete(Success(Done)))
    Done
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
  private def update[T <: StepListError](stepListResultFunc: ⇒ Either[T, StepList]) = async {
    val stepListResult = stepListResultFunc
    stepListResult.map { s ⇒
      stepList = s
      checkForSequenceCompletion()
      completeStepRefPromise()
      Done
    }
  }
}
