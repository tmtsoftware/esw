package esw.ocs.api.protocol

sealed trait SequencerWebsocketRequest

private[ocs] object SequencerWebsocketRequest {
  case object QueryFinal extends SequencerWebsocketRequest
}
