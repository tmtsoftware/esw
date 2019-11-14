package esw.ocs.api

import csw.params.commands.CommandResponse.{Started, SubmitResponse}
import csw.params.commands.Sequence
import csw.time.core.models.UTCTime
import esw.ocs.api.protocol._

import scala.concurrent.{ExecutionContext, Future}

trait SequencerCommandApi {
  protected implicit def executionContext: ExecutionContext

  def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse]
  def startSequence(): Future[SubmitResponse]
  def submit(sequence: Sequence): Future[SubmitResponse]

  def submitAndWait(sequence: Sequence): Future[SubmitResponse] = {
    submit(sequence).flatMap {
      case Started(_) => queryFinal()
      case x          => Future.successful(x)
    }
  }

  def queryFinal(): Future[SubmitResponse]

  def goOnline(): Future[GoOnlineResponse]
  def goOffline(): Future[GoOfflineResponse]
  def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse]
  def operationsMode(): Future[OperationsModeResponse]

}
