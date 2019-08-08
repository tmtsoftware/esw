package esw.ocs.core

import akka.Done
import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.models.messages.EditorError
import esw.ocs.api.models.messages.EditorError._
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.dsl.Async.{async, await}
import esw.ocs.macros.StrandEc

import scala.concurrent.{ExecutionContext, Future, Promise}

private[ocs] class Sequencer(crm: CommandResponseManager)(implicit strandEc: StrandEc, timeout: Timeout) {
  private implicit val singleThreadedEc: ExecutionContext = strandEc.ec
  private val emptyChildId                                = Id("empty-child") // fixme

  private var stepList                                         = StepList.empty
  private var loadedStepList                                   = StepList.empty
  private var previousStepList: Option[StepList]               = None
  private var readyToExecuteNextPromise: Option[Promise[Done]] = None
  private var stepRefPromise: Option[Promise[Step]]            = None
  private var sequencerAvailable                               = true
  private var online                                           = true

  def isOnline: Future[Boolean] = async(online)

  def goOnline(): Future[Either[GoOnlineError, Done]] = async {
    online = true
    Right(Done)
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

  def addBreakpoint(id: Id): Future[Either[AddBreakpointError, Done]] = updateStepListResult(stepList.addBreakpoint(id))

  def removeBreakpoint(id: Id): Future[Either[RemoveBreakpointError, Done]] =
    updateStepListResult(stepList.removeBreakpoint(id))

  private def createReadyToExecuteNextPromise() =
    createPromise[Done](p => readyToExecuteNextPromise = Some(p))

  // stepListResultFunc is by name because all StepList operations must execute on strandEc
  private def updateStepListResult[T <: EditorError](stepListResultFunc: => Either[T, StepList]) = async {
    val stepListResult = stepListResultFunc
    stepListResult.map { s =>
      stepList = s
      checkForSequenceCompletion()
      completeStepRefPromise()
      Done
    }
  }
}
