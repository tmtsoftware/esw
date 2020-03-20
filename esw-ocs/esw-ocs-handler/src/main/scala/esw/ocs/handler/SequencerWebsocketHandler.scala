package esw.ocs.handler

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import esw.ocs.api.SequencerApi
import esw.ocs.api.codecs.SequencerHttpCodecs._
import esw.ocs.api.protocol.SequencerWebsocketRequest
import esw.ocs.api.protocol.SequencerWebsocketRequest.QueryFinal
import msocket.api.ContentType
import msocket.impl.ws.WebsocketHandler

class SequencerWebsocketHandler(sequencerApi: SequencerApi, contentType: ContentType)
    extends WebsocketHandler[SequencerWebsocketRequest](contentType) {

  override def handle(request: SequencerWebsocketRequest): Source[Message, NotUsed] = request match {
    case QueryFinal(sequenceId, timeout) => stream(sequencerApi.queryFinal(sequenceId)(timeout))
  }
}
