package esw.ocs.api.protocol

import akka.util.Timeout
import csw.params.core.models.Id

/**
 * These models are being used to make websocket request for the sequencer
 */
sealed trait SequencerStreamRequest

// Sequencer Command Protocol
object SequencerStreamRequest {
  case class QueryFinal(runId: Id, timeout: Timeout) extends SequencerStreamRequest
  case object SubscribeSequencerState                extends SequencerStreamRequest
}
