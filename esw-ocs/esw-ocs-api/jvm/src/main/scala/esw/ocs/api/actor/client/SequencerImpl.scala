package esw.ocs.api.actor.client

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.api.utils.SequencerCommandServiceExtension
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.{Query, QueryFinal}
import csw.location.api.models.AkkaLocation
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.messages.SequencerMessages._
import esw.ocs.api.actor.messages.SequencerState
import esw.ocs.api.actor.messages.SequencerState.{Idle, Offline}
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class SequencerImpl(sequencer: ActorRef[SequencerMsg])(implicit system: ActorSystem[_]) extends SequencerApi {
  private implicit val timeout: Timeout     = SequenceApiTimeout.AskTimeout
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
  override def abortSequence(): Future[OkOrUnhandledResponse]             = sequencer ? AbortSequence
  override def stop(): Future[OkOrUnhandledResponse]                      = sequencer ? Stop

  override def isAvailable: Future[Boolean] = getState.map(_ == Idle)

  override def isOnline: Future[Boolean] = getState.map(_ != Offline)

  private def getState: Future[SequencerState[SequencerMsg]] = sequencer ? GetSequencerState

  // commands

  override def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse] =
    sequencer ? (LoadSequence(sequence, _))

  override def startSequence(): Future[SubmitResponse] = {
    val sequenceResponse: Future[SequencerSubmitResponse] = sequencer ? StartSequence
    sequenceResponse.map(_.toSubmitResponse())
  }

  override def submit(sequence: Sequence): Future[SubmitResponse] = {
    val sequenceResponseF: Future[SequencerSubmitResponse] = sequencer ? (SubmitSequenceInternal(sequence, _))
    sequenceResponseF.map(_.toSubmitResponse())
  }

  override def submitAndWait(sequence: Sequence)(implicit timeout: Timeout): Future[SubmitResponse] =
    extensions.submitAndWait(sequence)

  override def query(runId: Id): Future[SubmitResponse] = sequencer ? (Query(runId, _))

  override def queryFinal(runId: Id)(implicit timeout: Timeout): Future[SubmitResponse] = sequencer ? (QueryFinal(runId, _))

  override def goOnline(): Future[GoOnlineResponse] = sequencer ? GoOnline

  override def goOffline(): Future[GoOfflineResponse] = sequencer ? GoOffline

  override def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse] =
    sequencer ? (DiagnosticMode(startTime, hint, _))

  override def operationsMode(): Future[OperationsModeResponse] = sequencer ? OperationsMode

  override def getSequenceComponent: Future[AkkaLocation] = sequencer ? GetSequenceComponent
}

object SequenceApiTimeout {
  val AskTimeout: Timeout = 5.seconds
}
