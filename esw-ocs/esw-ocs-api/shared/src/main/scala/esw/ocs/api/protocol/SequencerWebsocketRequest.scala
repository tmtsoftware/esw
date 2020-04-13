package esw.ocs.api.protocol

import akka.util.Timeout
import csw.params.core.models.Id

sealed trait SequencerWebsocketRequest

// Sequencer Command Protocol
object SequencerWebsocketRequest {
  case class QueryFinal(runId: Id, timeout: Timeout) extends SequencerWebsocketRequest
}
