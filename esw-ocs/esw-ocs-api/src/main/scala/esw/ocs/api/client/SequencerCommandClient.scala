package esw.ocs.api.client

import csw.params.commands.CommandResponse.{QueryResponse, SubmitResponse}
import csw.params.commands.Sequence
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import esw.ocs.api.{SequencerCommandApi, SequencerCommandExtensions}
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerPostRequest._
import esw.ocs.api.protocol.SequencerWebsocketRequest.QueryFinal
import esw.ocs.api.protocol._
import msocket.api.Transport

import scala.concurrent.{ExecutionContext, Future}

class SequencerCommandClient(
    postClient: Transport[SequencerPostRequest],
    websocketClient: Transport[SequencerWebsocketRequest]
)(implicit ec: ExecutionContext)
    extends SequencerCommandApi
    with SequencerHttpCodecs {

  private val extensions = new SequencerCommandExtensions(this)

  override def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse] =
    postClient.requestResponse[OkOrUnhandledResponse](LoadSequence(sequence))

  override def startSequence(): Future[SubmitResponse] = postClient.requestResponse[SubmitResponse](StartSequence)

  override def submit(sequence: Sequence): Future[SubmitResponse] =
    postClient.requestResponse[SubmitResponse](SubmitSequence(sequence))

  override def submitAndWait(sequence: Sequence): Future[SubmitResponse] = extensions.submitAndWait(sequence)

  override def query(runId: Id): Future[QueryResponse] =
    postClient.requestResponse[QueryResponse](Query(runId))

  override def queryFinal(runId: Id): Future[SubmitResponse] =
    websocketClient.requestResponse[SubmitResponse](QueryFinal(runId))

  override def goOnline(): Future[GoOnlineResponse] = postClient.requestResponse[GoOnlineResponse](GoOnline)

  override def goOffline(): Future[GoOfflineResponse] = postClient.requestResponse[GoOfflineResponse](GoOffline)

  override def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse] =
    postClient.requestResponse[DiagnosticModeResponse](DiagnosticMode(startTime, hint))

  override def operationsMode(): Future[OperationsModeResponse] =
    postClient.requestResponse[OperationsModeResponse](OperationsMode)
}
