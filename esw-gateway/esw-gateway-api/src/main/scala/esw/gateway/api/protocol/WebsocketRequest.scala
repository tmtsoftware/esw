package esw.gateway.api.protocol

import csw.location.models.ComponentId
import csw.params.core.models.{Id, Subsystem}
import csw.params.core.states.StateName
import csw.params.events.EventKey

sealed trait WebsocketRequest

object WebsocketRequest {
  case class QueryFinal(componentId: ComponentId, runId: Id) extends WebsocketRequest
  case class SubscribeCurrentState(
      componentId: ComponentId,
      stateNames: Set[StateName] = Set.empty,
      maxFrequency: Option[Int] = None
  ) extends WebsocketRequest

  case class Subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int] = None) extends WebsocketRequest
  case class SubscribeWithPattern(subsystem: Subsystem, maxFrequency: Option[Int] = None, pattern: String = "*")
      extends WebsocketRequest
}
