package esw.ocs.app.route

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.protocol.SequencerAdminWebsocketRequest
import esw.ocs.api.protocol.SequencerAdminWebsocketRequest.QueryFinal
import mscoket.impl.ws.WebsocketStreamExtensions
import msocket.api.RequestHandler

class WebsocketHandlerImpl(sequencerAdmin: SequencerAdminApi)
    extends RequestHandler[SequencerAdminWebsocketRequest, Source[Message, NotUsed]]
    with SequencerAdminHttpCodecs
    with WebsocketStreamExtensions {

  override def handle(request: SequencerAdminWebsocketRequest): Source[Message, NotUsed] = request match {
    case QueryFinal => futureAsStream(sequencerAdmin.queryFinal)
  }
}
