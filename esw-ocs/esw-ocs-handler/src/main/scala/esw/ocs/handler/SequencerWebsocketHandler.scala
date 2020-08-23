package esw.ocs.handler

import esw.ocs.api.SequencerApi
import esw.ocs.api.codecs.SequencerHttpCodecs._
import esw.ocs.api.protocol.SequencerWebsocketRequest
import esw.ocs.api.protocol.SequencerWebsocketRequest.QueryFinal
import msocket.api.{StreamRequestHandler, StreamResponse}

import scala.concurrent.Future

class SequencerWebsocketHandler(sequencerApi: SequencerApi) extends StreamRequestHandler[SequencerWebsocketRequest] {

  override def handle(request: SequencerWebsocketRequest): Future[StreamResponse] =
    request match {
      case QueryFinal(sequenceId, timeout) => future(sequencerApi.queryFinal(sequenceId)(timeout))
    }
}
