package esw.ocs.api.client

import csw.params.commands.CommandResponse.SubmitResponse
import esw.ocs.api.SequencerCommandApi
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerCommandWebsocketRequest
import esw.ocs.api.protocol.SequencerCommandWebsocketRequest.QueryFinal
import msocket.api.Transport

import scala.concurrent.Future

class SequencerCommandClient(
    websocketClient: Transport[SequencerCommandWebsocketRequest]
) extends SequencerCommandApi
    with SequencerHttpCodecs {

  override def queryFinal: Future[SubmitResponse] = {
    websocketClient.requestResponse[SubmitResponse](QueryFinal)
  }
}
