package esw.ocs.api.protocol

sealed trait SequencerCommandWebsocketRequest

private[ocs] object SequencerCommandWebsocketRequest {
  case object QueryFinal extends SequencerCommandWebsocketRequest
}
