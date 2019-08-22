package esw.gateway.api.messages

import csw.location.models.ComponentId
import csw.params.core.models.{Id, Subsystem}
import csw.params.core.states.StateName
import csw.params.events.EventKey

sealed trait GatewayWebsocketRequest

object GatewayWebsocketRequest {
  case class QueryFinal(componentId: ComponentId, runId: Id) extends GatewayWebsocketRequest
  case class SubscribeCurrentState(componentId: ComponentId, stateNames: Set[StateName], maxFrequency: Option[Int])
      extends GatewayWebsocketRequest

  case class Subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int] = None) extends GatewayWebsocketRequest
  case class SubscribeWithPattern(subsystem: Subsystem, maxFrequency: Option[Int], pattern: String = "*")
      extends GatewayWebsocketRequest
}
