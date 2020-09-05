package esw.ocs.handler

import esw.ocs.api.SequencerApi
import esw.ocs.api.codecs.SequencerServiceCodecs._
import esw.ocs.api.protocol.SequencerStreamRequest
import esw.ocs.api.protocol.SequencerStreamRequest.QueryFinal
import msocket.jvm.stream.{StreamRequestHandler, StreamResponse}

import scala.concurrent.Future

class SequencerWebsocketHandler(sequencerApi: SequencerApi) extends StreamRequestHandler[SequencerStreamRequest] {

  override def handle(request: SequencerStreamRequest): Future[StreamResponse] =
    request match {
      case QueryFinal(sequenceId, timeout) => future(sequencerApi.queryFinal(sequenceId)(timeout))
    }
}
