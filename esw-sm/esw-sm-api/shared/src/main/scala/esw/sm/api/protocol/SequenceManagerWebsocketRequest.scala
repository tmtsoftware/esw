package esw.sm.api.protocol

sealed trait SequenceManagerWebsocketRequest

object SequenceManagerWebsocketRequest {

  case class Configure(obsMode: String) extends SequenceManagerWebsocketRequest
}
