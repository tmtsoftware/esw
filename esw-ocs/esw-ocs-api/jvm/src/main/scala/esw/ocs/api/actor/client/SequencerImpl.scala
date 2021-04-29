package esw.ocs.api.actor.client

import akka.actor.typed.scaladsl.AskPattern._
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
import esw.ocs.api.actor.messages.SequencerMessages._
import esw.ocs.api.actor.messages.SequencerState
import esw.ocs.api.actor.messages.SequencerState.{Idle, Loaded, Offline, Running}
import esw.ocs.api.models.{ExternalSequencerState, StepList}
import esw.ocs.api.protocol._
import msocket.api.Subscription
import msocket.jvm.SourceExtension.WithSubscription

import scala.concurrent.{ExecutionContext, Future}

class SequencerImpl(sequencer: ActorRef[SequencerMsg])(implicit system: ActorSystem[_]) extends SequencerApi {
  private implicit val timeout: Timeout     = SequencerTimeouts.SequencerOperation
  private implicit val ec: ExecutionContext = system.executionContext

  private val extensions = new SequencerCommandServiceExtension(this)

  override def getSequence: Future[Option[StepList]] = sequencer ? GetSequence

  override def add(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse]       = sequencer ? (Add(commands, _))
  override def prepend(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse]   = sequencer ? (Prepend(commands, _))
  override def replace(id: Id, commands: List[SequenceCommand]): Future[GenericResponse] = sequencer ? (Replace(id, commands, _))

  override def insertAfter(id: Id, commands: List[SequenceCommand]): Future[GenericResponse] =
    sequencer ? (InsertAfter(id, commands, _))

  override def delete(id: Id): Future[GenericResponse]                    = sequencer ? (Delete(id, _))
  override def pause: Future[PauseResponse]                               = sequencer ? Pause
  override def resume: Future[OkOrUnhandledResponse]                      = sequencer ? Resume
  override def addBreakpoint(id: Id): Future[GenericResponse]             = sequencer ? (AddBreakpoint(id, _))
  override def removeBreakpoint(id: Id): Future[RemoveBreakpointResponse] = sequencer ? (RemoveBreakpoint(id, _))
  override def reset(): Future[OkOrUnhandledResponse]                     = sequencer ? Reset
  override def abortSequence(): Future[OkOrUnhandledResponse] = {
    implicit val timeout: Timeout = SequencerTimeouts.ScriptHandlerExecution
    sequencer ? AbortSequence
  }

  override def stop(): Future[OkOrUnhandledResponse] = {
    implicit val timeout: Timeout = SequencerTimeouts.ScriptHandlerExecution
    sequencer ? Stop
  }

  override def isAvailable: Future[Boolean] = getState.map(_ == Idle)

  override def isOnline: Future[Boolean] = getState.map(_ != Offline)

  private def getState: Future[SequencerState[SequencerMsg]] = sequencer ? GetSequencerState

  def getSequencerState: Future[ExternalSequencerState] =
    getState.map {
      case Idle    => ExternalSequencerState.Idle
      case Loaded  => ExternalSequencerState.Loaded
      case Running => ExternalSequencerState.Running
      case Offline => ExternalSequencerState.Offline
      case _       => ExternalSequencerState.Processing
    }

  override def subscribeSequencerState(): Source[SequencerStateResponse, Subscription] =
    ActorSource
      .actorRef[SequencerStateResponse](
        PartialFunction.empty,
        PartialFunction.empty,
        16,
        OverflowStrategy.dropHead
      )
      .mapMaterializedValue { sequencer ! SubscribeSequencerState(_) }
      .withSubscription()
//      .distinctUntilChange // todo: expose it from event service

  // todo : unsubscribe commands

  override def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse] =
    sequencer ? (LoadSequence(sequence, _))

  override def startSequence(): Future[SubmitResponse] = {
    implicit val timeout: Timeout                         = SequencerTimeouts.ScriptHandlerExecution
    val sequenceResponse: Future[SequencerSubmitResponse] = sequencer ? StartSequence
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
    sequencer ? GoOnline
  }

  override def goOffline(): Future[GoOfflineResponse] = {
    implicit val timeout: Timeout = SequencerTimeouts.ScriptHandlerExecution
    sequencer ? GoOffline
  }

  override def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse] = {
    implicit val timeout: Timeout = SequencerTimeouts.ScriptHandlerExecution
    sequencer ? (DiagnosticMode(startTime, hint, _))
  }

  override def operationsMode(): Future[OperationsModeResponse] = {
    implicit val timeout: Timeout = SequencerTimeouts.ScriptHandlerExecution
    sequencer ? OperationsMode
  }

  override def getSequenceComponent: Future[AkkaLocation] = sequencer ? GetSequenceComponent
}
