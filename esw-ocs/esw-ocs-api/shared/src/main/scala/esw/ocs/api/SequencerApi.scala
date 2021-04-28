package esw.ocs.api

import akka.stream.scaladsl.Source
import csw.command.api.scaladsl.SequencerCommandService
import csw.location.api.models.AkkaLocation
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{Sequence, SequenceCommand}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol._

import scala.concurrent.Future

trait SequencerApi extends SequencerCommandService {

  def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse]
  def startSequence(): Future[SubmitResponse]

  def getSequence: Future[Option[StepList]]
  def add(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse]
  def prepend(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse]
  def replace(id: Id, commands: List[SequenceCommand]): Future[GenericResponse]
  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[GenericResponse]
  def delete(id: Id): Future[GenericResponse]
  def addBreakpoint(id: Id): Future[GenericResponse]
  def removeBreakpoint(id: Id): Future[RemoveBreakpointResponse]
  def reset(): Future[OkOrUnhandledResponse]
  def pause: Future[PauseResponse]
  def resume: Future[OkOrUnhandledResponse]
  def getSequenceComponent: Future[AkkaLocation]

  def isAvailable: Future[Boolean]
  def isOnline: Future[Boolean]
  def goOnline(): Future[GoOnlineResponse]
  def goOffline(): Future[GoOfflineResponse]
  def abortSequence(): Future[OkOrUnhandledResponse]
  def stop(): Future[OkOrUnhandledResponse]
  def getSequencerState: Future[ExternalSequencerState]
  def subscribeSequencerState(): Source[SequencerStateResponse, Unit]

  def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse]
  def operationsMode(): Future[OperationsModeResponse]
}
