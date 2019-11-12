package esw.ocs.app.route

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import esw.ocs.api.SequencerCommandApi
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerCommandWebsocketRequest
import esw.ocs.api.protocol.SequencerCommandWebsocketRequest.QueryFinal
import msocket.api.MessageHandler
import msocket.impl.Encoding
import msocket.impl.ws.WebsocketStreamExtensions

class WebsocketHandlerImpl(sequencerCommandApi: SequencerCommandApi, val encoding: Encoding[_])
    extends MessageHandler[SequencerCommandWebsocketRequest, Source[Message, NotUsed]]
    with SequencerHttpCodecs
    with WebsocketStreamExtensions {

  override def handle(request: SequencerCommandWebsocketRequest): Source[Message, NotUsed] = request match {
    case QueryFinal => futureAsStream(sequencerCommandApi.queryFinal)
  }
}
