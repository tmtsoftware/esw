package esw.ocs.impl

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol._
import esw.ocs.impl.messages.SequencerMessages._
import esw.ocs.impl.messages.SequencerState
import esw.ocs.impl.messages.SequencerState.{Idle, Offline}

import scala.concurrent.{ExecutionContext, Future}

class SequencerAdminImpl(sequencer: ActorRef[EswSequencerMessage])(implicit system: ActorSystem[_], timeout: Timeout)
    extends SequencerAdminApi {
  private implicit val scheduler: Scheduler = system.scheduler
  private implicit val ec: ExecutionContext = system.executionContext

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
  override def goOnline(): Future[GoOnlineResponse]                       = sequencer ? GoOnline
  override def goOffline(): Future[GoOfflineResponse]                     = sequencer ? GoOffline

  override def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse] =
    sequencer ? (DiagnosticMode(startTime, hint, _))
  override def operationsMode(): Future[OperationsModeResponse] = sequencer ? OperationsMode

  override def isAvailable: Future[Boolean] = getState.map(_ == Idle)
  override def isOnline: Future[Boolean]    = getState.map(_ != Offline)

  private def getState: Future[SequencerState[SequencerMsg]] = sequencer ? GetSequencerState

  override def loadSequence(sequence: Sequence): Future[LoadSequenceResponse] = sequencer ? (LoadSequence(sequence, _))

  override def startSequence: Future[OkOrUnhandledResponse] = sequencer ? StartSequence

  override def submitSequence(sequence: Sequence): Future[LoadSequenceResponse] =
    sequencer ? (SubmitSequence(sequence, _))

  // fixme: shouldn't this call have long timeout and not the default?
  override def queryFinal: Future[SequenceResponse] = sequencer ? QueryFinal
}
