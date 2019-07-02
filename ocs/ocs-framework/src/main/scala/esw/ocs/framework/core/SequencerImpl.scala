package esw.ocs.framework.core

import akka.Done
import akka.util.Timeout
import cats.implicits._
import csw.command.client.CommandResponseManager
import csw.params.commands.CommandResponse.{Started, SubmitResponse}
import csw.params.commands.{CommandResponse, SequenceCommand}
import csw.params.core.models.Id

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

// todo: experimental version without actor
private[framework] class SequencerImpl(crm: CommandResponseManager)(implicit strandEc: StrandEc, timeout: Timeout) {
  private implicit val singleThreadedEc: ExecutionContext = strandEc.ec

  private var stepList                                         = StepList.empty
  private var latestResponse: Option[SubmitResponse]           = None
  private var readyToExecuteNextPromise: Option[Promise[Done]] = None
  private var stepRefPromise: Option[Promise[Step]]            = None

  private val emptyChildId = Id("empty-child")

  def processSequence(sequence: Sequence): Future[Either[ProcessSequenceError, SubmitResponse]] =
    if (stepList.isFinished)
      StepList(sequence)
        .map { sl ⇒
          val id = sl.runId
          stepList = sl
          crm.addOrUpdateCommand(Started(id))
          crm.addSubCommand(id, emptyChildId)
          crm.queryFinal(id)
        }
        .leftMap(Future.successful)
        .bisequence
    else Future.successful(Left(ExistingSequenceIsInProcess))

  def pullNext(): Future[Step] =
    stepList.nextExecutable match {
      case Some(step) ⇒ Future(setInFlight(step))
      case None ⇒
        val p = Promise[Step]()
        stepRefPromise = Some(p)
        p.future
    }

  private def setInFlight0(step: Step): Either[UpdateResponseError, Step] =
    stepList
      .updateStatus0(step.id, InFlight)
      .map { sl ⇒
        stepList = sl
        val stepRunId = step.id
        crm.addSubCommand(stepList.runId, stepRunId)
        crm.addOrUpdateCommand(CommandResponse.Started(stepRunId))
        crm.queryFinal(stepRunId).foreach(update)
        step
      }

  private def setInFlight(step: Step) = {
    val stepListResult = stepList.updateStatus(step.id, InFlight)

    stepListResult.response match {
      case Updated(step) ⇒
        val stepRunId = step.id
        crm.addSubCommand(stepList.runId, stepRunId)
        crm.addOrUpdateCommand(CommandResponse.Started(stepRunId))
        crm.queryFinal(stepRunId).foreach(update)
        step
      case x @ NotAllowedOnFinishedSeq ⇒ throw x
      case x @ IdDoesNotExist(id)      ⇒ throw x
      case x @ UpdateFailed            ⇒ throw x
    }
  }

  def update(submitResponse: SubmitResponse): Future[UpdateResponse] = Future {
    crm.updateSubCommand(CommandResponse.withRunId(submitResponse.runId, submitResponse))

    //fixme: What if SubmitResponse is negative
    val stepListResult = stepList.updateStatus(submitResponse.runId, Finished)
    stepList = stepListResult.stepList
    latestResponse = Some(submitResponse)
    clearIfSequenceFinished()
    stepListResult.response
  }

  private def clearIfSequenceFinished(): Unit =
    if (isSequenceFinished) {
      val sequenceResponse = CommandResponse.withRunId(stepList.runId, latestResponse.orNull) //whether this will be called with None latestresponse ever??
      crm.updateSubCommand(CommandResponse.withRunId(emptyChildId, sequenceResponse))
      // fixme: Confirm
      readyToExecuteNextPromise.foreach(_.complete(Success(Done)))
      stepList = StepList.empty
      latestResponse = None
      readyToExecuteNextPromise = None
    }

  private def isSequenceFinished: Boolean =
    stepList.isFinished || latestResponse.exists(_.runId == stepList.runId)

  def getSequence: Future[StepList]   = Future(stepList) // any harm doing Future.successful?
  def mayBeNext: Future[Option[Step]] = Future(stepList.nextExecutable)

  def add(commands: List[SequenceCommand]): Future[AddResponse]                 = update(stepList.append(commands))
  def pause: Future[PauseResponse]                                              = update(stepList.pause)
  def resume: Future[ResumeResponse]                                            = update(stepList.resume)
  def discardPending: Future[DiscardPendingResponse]                            = update(stepList.discardPending)
  def replace(id: Id, commands: List[SequenceCommand]): Future[ReplaceResponse] = update(stepList.replace(id, commands))
  def prepend(commands: List[SequenceCommand]): Future[PrependResponse]         = update(stepList.prepend(commands))
  def delete(ids: List[Id]): Future[DeleteResponse]                             = update(stepList.delete(ids.toSet))
  def addBreakpoints(ids: List[Id]): Future[AddBreakpointsResponse]             = update(stepList.addBreakpoints(ids))
  def removeBreakpoints(ids: List[Id]): Future[RemoveBreakpointsResponse]       = update(stepList.removeBreakpoints(ids))
  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[InsertAfterResponse] =
    update(stepList.insertAfter(id, commands))

  def readyToExecuteNext: Future[Done] =
    if (!stepList.isInFlight) Future.successful(Done)
    else {
      val p = Promise[Done]()
      readyToExecuteNextPromise = Some(p)
      p.future
    }

  private def trySend(): Future[Unit] = async {
    for {
      ref  <- stepRefPromise
      step <- stepList.nextExecutable
    } {
      ref.complete(Success(setInFlight(step)))
      stepRefPromise = None
    }
  }

  private def update[T <: StepListActionResponse](stepListResult: StepListResult[T]): Future[T] = Future {
    stepList = stepListResult.stepList
    clearIfSequenceFinished()
    trySend()
    stepListResult.response
  }

}
