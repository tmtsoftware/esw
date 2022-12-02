package esw.ocs.api.actor.client

import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorSource
import akka.util.Timeout
import csw.command.api.utils.SequencerCommandServiceExtension
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.{Query, QueryFinal}
import csw.location.api.models.AkkaLocation
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import esw.constants.SequencerTimeouts
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.messages.InternalSequencerState
import esw.ocs.api.actor.messages.InternalSequencerState.{Idle, Loaded, Offline, Running}
import esw.ocs.api.actor.messages.SequencerMessages.*
import esw.ocs.api.models.{SequencerState, StepList}
import esw.ocs.api.protocol.*
import esw.ocs.api.protocol.SequencerStateSubscriptionResponse.SequencerShuttingDown
import msocket.api.Subscription
import msocket.jvm.SourceExtension.RichSource

import scala.concurrent.{ExecutionContext, Future}

/**
 * Actor client for the sequencer. This client's apis sends message to sequencer actor
 * and returned the response provided by sequencer
 * This client takes actor ref of the sequencer as a constructor argument
 *
 * @param sequencer - actorRef of the Sequencer Actor
 * @param system - an Akka ActorSystem
 */
class SequencerImpl(sequencer: ActorRef[SequencerMsg])(implicit system: ActorSystem[_]) extends SequencerApi {
  private implicit val timeout: Timeout     = SequencerTimeouts.SequencerOperation
  private implicit val ec: ExecutionContext = system.executionContext

  private val extensions = new SequencerCommandServiceExtension(this)

  override def getSequence: Future[Option[StepList]] = sequencer ? GetSequence.apply

  override def add(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse]       = sequencer ? (Add(commands, _))
  override def prepend(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse]   = sequencer ? (Prepend(commands, _))
  override def replace(id: Id, commands: List[SequenceCommand]): Future[GenericResponse] = sequencer ? (Replace(id, commands, _))

  override def insertAfter(id: Id, commands: List[SequenceCommand]): Future[GenericResponse] =
    sequencer ? (InsertAfter(id, commands, _))

  override def delete(id: Id): Future[GenericResponse]                    = sequencer ? (Delete(id, _))
  override def pause: Future[PauseResponse]                               = sequencer ? Pause.apply
  override def resume: Future[OkOrUnhandledResponse]                      = sequencer ? Resume.apply
  override def addBreakpoint(id: Id): Future[GenericResponse]             = sequencer ? (AddBreakpoint(id, _))
  override def removeBreakpoint(id: Id): Future[RemoveBreakpointResponse] = sequencer ? (RemoveBreakpoint(id, _))
  override def reset(): Future[OkOrUnhandledResponse]                     = sequencer ? Reset.apply
  override def abortSequence(): Future[OkOrUnhandledResponse] = {
    implicit val timeout: Timeout = SequencerTimeouts.ScriptHandlerExecution
    sequencer ? AbortSequence.apply
  }

  override def stop(): Future[OkOrUnhandledResponse] = {
    implicit val timeout: Timeout = SequencerTimeouts.ScriptHandlerExecution
    sequencer ? Stop.apply
  }

  override def isAvailable: Future[Boolean] = getState.map(_ == Idle)

  override def isOnline: Future[Boolean] = getState.map(_ != Offline)

  private def getState: Future[InternalSequencerState[SequencerMsg]] = sequencer ? GetSequencerState.apply

  def getSequencerState: Future[SequencerState] =
    getState.map {
      case Idle    => SequencerState.Idle
      case Loaded  => SequencerState.Loaded
      case Running => SequencerState.Running
      case Offline => SequencerState.Offline
      case _       => SequencerState.Processing
    }

  override def subscribeSequencerState(): Source[SequencerStateResponse, Subscription] =
    ActorSource
      .actorRef[SequencerStateSubscriptionResponse](
        completionMatcher = { case SequencerShuttingDown =>
        },
        PartialFunction.empty,
        16,
        OverflowStrategy.dropHead
      )
      .collect { case sequencerState: SequencerStateResponse =>
        sequencerState
      }
      .watchTermination() { (ref, doneF) =>
        sequencer ! SubscribeSequencerState(ref)
        doneF.onComplete(_ => sequencer ! UnsubscribeSequencerState(ref))
      }
      .withSubscription()
      .distinctUntilChanged

  override def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse] =
    sequencer ? (LoadSequence(sequence, _))

  override def startSequence(): Future[SubmitResponse] = {
    implicit val timeout: Timeout                         = SequencerTimeouts.ScriptHandlerExecution
    val sequenceResponse: Future[SequencerSubmitResponse] = sequencer ? StartSequence.apply
    sequenceResponse.map(_.toSubmitResponse())
  }

  override def submit(sequence: Sequence): Future[SubmitResponse] = {
    implicit val timeout: Timeout                          = SequencerTimeouts.ScriptHandlerExecution
    val sequenceResponseF: Future[SequencerSubmitResponse] = sequencer ? (SubmitSequenceInternal(sequence, _))
    sequenceResponseF.map(_.toSubmitResponse())
  }

  override def submitAndWait(sequence: Sequence)(implicit timeout: Timeout): Future[SubmitResponse] =
    extensions.submitAndWait(sequence)

  override def query(runId: Id): Future[SubmitResponse] = sequencer ? (Query(runId, _))

  override def queryFinal(runId: Id)(implicit timeout: Timeout): Future[SubmitResponse] = sequencer ? (QueryFinal(runId, _))

  override def goOnline(): Future[GoOnlineResponse] = {
    implicit val timeout: Timeout = SequencerTimeouts.ScriptHandlerExecution
    sequencer ? GoOnline.apply
  }

  override def goOffline(): Future[GoOfflineResponse] = {
    implicit val timeout: Timeout = SequencerTimeouts.ScriptHandlerExecution
    sequencer ? GoOffline.apply
  }

  override def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse] = {
    implicit val timeout: Timeout = SequencerTimeouts.ScriptHandlerExecution
    sequencer ? (DiagnosticMode(startTime, hint, _))
  }

  override def operationsMode(): Future[OperationsModeResponse] = {
    implicit val timeout: Timeout = SequencerTimeouts.ScriptHandlerExecution
    sequencer ? OperationsMode.apply
  }

  override def getSequenceComponent: Future[AkkaLocation] = sequencer ? GetSequenceComponent.apply
}
