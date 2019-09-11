package esw.ocs.api.request

sealed trait SequencerAdminWebsocketRequest

object SequencerAdminWebsocketRequest {
  case object QueryFinal extends SequencerAdminWebsocketRequest
}
