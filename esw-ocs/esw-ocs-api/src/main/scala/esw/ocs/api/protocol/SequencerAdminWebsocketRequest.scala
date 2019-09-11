package esw.ocs.api.protocol

sealed trait SequencerAdminWebsocketRequest

private[ocs] object SequencerAdminWebsocketRequest {
  case object QueryFinal extends SequencerAdminWebsocketRequest
}
