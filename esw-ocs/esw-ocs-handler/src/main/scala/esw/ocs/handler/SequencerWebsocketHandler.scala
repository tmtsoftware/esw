package esw.ocs.handler

import esw.ocs.api.SequencerApi
import esw.ocs.api.codecs.SequencerServiceCodecs.*
import esw.ocs.api.protocol.SequencerStreamRequest
import esw.ocs.api.protocol.SequencerStreamRequest.QueryFinal
import msocket.jvm.stream.{StreamRequestHandler, StreamResponse}

import scala.concurrent.Future

/**
 * This is the Websocket route handler written using msocket apis for Sequencer.
 *
 * @param sequencerApi - an instance of sequencerApi of the sequencer
 */
class SequencerWebsocketHandler(sequencerApi: SequencerApi) extends StreamRequestHandler[SequencerStreamRequest] {

  override def handle(request: SequencerStreamRequest): Future[StreamResponse] =
    request match {
      case QueryFinal(sequenceId, timeout)                => response(sequencerApi.queryFinal(sequenceId)(timeout))
      case SequencerStreamRequest.SubscribeSequencerState => stream(sequencerApi.subscribeSequencerState())
    }
}
