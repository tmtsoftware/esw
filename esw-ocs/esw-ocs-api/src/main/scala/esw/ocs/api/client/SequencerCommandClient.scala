package esw.ocs.api.client

import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import esw.ocs.api.SequencerCommandApi
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerCommandPostRequest.{LoadSequence, StartSequence, SubmitSequence}
import esw.ocs.api.protocol.SequencerCommandWebsocketRequest.QueryFinal
import esw.ocs.api.protocol.{OkOrUnhandledResponse, SequencerCommandPostRequest, SequencerCommandWebsocketRequest}
import msocket.api.Transport

import scala.concurrent.Future

class SequencerCommandClient(
    postClient: Transport[SequencerCommandPostRequest],
    websocketClient: Transport[SequencerCommandWebsocketRequest]
) extends SequencerCommandApi
    with SequencerHttpCodecs {

  override def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse] =
    postClient.requestResponse[OkOrUnhandledResponse](LoadSequence(sequence))

  override def startSequence(): Future[SubmitResponse] = postClient.requestResponse[SubmitResponse](StartSequence)

  override def submit(sequence: Sequence): Future[SubmitResponse] =
    postClient.requestResponse[SubmitResponse](SubmitSequence(sequence))

//  override def submitAndWait(sequence: Sequence): Future[SubmitResponse] = submit(sequence).flatMap {
//    case Started(_) => queryFinal()
//    case x          => Future.successful(x)
//  }

  override def queryFinal(): Future[SubmitResponse] = websocketClient.requestResponse[SubmitResponse](QueryFinal)
}
