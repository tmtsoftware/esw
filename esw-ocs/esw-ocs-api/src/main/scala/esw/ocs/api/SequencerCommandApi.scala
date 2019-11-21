package esw.ocs.api

import csw.params.commands.CommandResponse.{QueryResponse, SubmitResponse}
import csw.params.commands.Sequence
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import esw.ocs.api.protocol._

import scala.concurrent.Future

trait SequencerCommandApi {
  def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse]
  def startSequence(): Future[SubmitResponse]
  def submit(sequence: Sequence): Future[SubmitResponse]
  def submitAndWait(sequence: Sequence): Future[SubmitResponse]
  def query(runId: Id): Future[QueryResponse]
  def queryFinal(runId: Id): Future[SubmitResponse]

  def goOnline(): Future[GoOnlineResponse]
  def goOffline(): Future[GoOfflineResponse]
  def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse]
  def operationsMode(): Future[OperationsModeResponse]
}
