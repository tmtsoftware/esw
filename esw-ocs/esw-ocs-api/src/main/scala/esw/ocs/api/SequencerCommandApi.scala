package esw.ocs.api

import csw.command.api.scaladsl.SequencerCommandService
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.time.core.models.UTCTime
import esw.ocs.api.protocol._

import scala.concurrent.Future

trait SequencerCommandApi extends SequencerCommandService {
  def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse]
  def startSequence(): Future[SubmitResponse]

  def goOnline(): Future[GoOnlineResponse]
  def goOffline(): Future[GoOfflineResponse]
  def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse]
  def operationsMode(): Future[OperationsModeResponse]
}
