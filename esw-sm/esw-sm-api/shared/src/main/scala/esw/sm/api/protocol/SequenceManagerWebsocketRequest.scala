package esw.sm.api.protocol

import akka.util.Timeout

sealed trait SequenceManagerWebsocketRequest

object SequenceManagerWebsocketRequest {

  case class Configure(obsMode: String, timeout: Timeout) extends SequenceManagerWebsocketRequest
}
