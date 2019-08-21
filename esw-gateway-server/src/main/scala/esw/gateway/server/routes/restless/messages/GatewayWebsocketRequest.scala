package esw.gateway.server.routes.restless.messages

import csw.location.models.ComponentType
import csw.params.core.models.{Id, Subsystem}
import csw.params.core.states.StateName
import csw.params.events.EventKey

sealed trait GatewayWebsocketRequest

object GatewayWebsocketRequest {
  case class QueryFinal(componentType: ComponentType, componentName: String, runId: Id) extends GatewayWebsocketRequest
  case class SubscribeCurrentState(
      componentType: ComponentType,
      componentName: String,
      stateNames: Set[StateName],
      maxFrequency: Option[Int]
  ) extends GatewayWebsocketRequest

  case class Subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int] = None) extends GatewayWebsocketRequest
  case class SubscribeWithPattern(subsystem: Subsystem, maxFrequency: Option[Int], pattern: String = "*")
      extends GatewayWebsocketRequest
}
