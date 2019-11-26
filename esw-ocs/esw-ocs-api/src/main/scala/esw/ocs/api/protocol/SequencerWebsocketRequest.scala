package esw.ocs.api.protocol

import akka.util.Timeout
import csw.params.core.models.Id

sealed trait SequencerWebsocketRequest

private[ocs] object SequencerWebsocketRequest {
  case class QueryFinal(runId: Id, timeout: Timeout) extends SequencerWebsocketRequest
  case object GetInsights                            extends SequencerWebsocketRequest
}
