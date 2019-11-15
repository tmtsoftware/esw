package esw.ocs.api.client

import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerCommandApi
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

  override implicit def executionContext: ExecutionContext = ec

  override def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse] =
    postClient.requestResponse[OkOrUnhandledResponse](LoadSequence(sequence))

  override def startSequence(): Future[SubmitResponse] = postClient.requestResponse[SubmitResponse](StartSequence)

  override def submit(sequence: Sequence): Future[SubmitResponse] =
    postClient.requestResponse[SubmitResponse](SubmitSequence(sequence))

  override def queryFinal(sequenceId: Id): Future[SubmitResponse] =
    websocketClient.requestResponse[SubmitResponse](QueryFinal(sequenceId))

  override def goOnline(): Future[GoOnlineResponse] = postClient.requestResponse[GoOnlineResponse](GoOnline)

  override def goOffline(): Future[GoOfflineResponse] = postClient.requestResponse[GoOfflineResponse](GoOffline)

  override def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse] =
    postClient.requestResponse[DiagnosticModeResponse](DiagnosticMode(startTime, hint))

  override def operationsMode(): Future[OperationsModeResponse] =
    postClient.requestResponse[OperationsModeResponse](OperationsMode)
}
