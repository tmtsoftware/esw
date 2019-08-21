package esw.gateway.server.routes.restless.messages

import csw.location.models.ComponentType
import csw.params.core.models.{Id, Subsystem}
import csw.params.core.states.StateName
import csw.params.events.EventKey

sealed trait WebSocketRequest

object WebSocketRequest {
  case class QueryFinal(componentType: ComponentType, componentName: String, runId: Id) extends WebSocketRequest
  case class SubscribeCurrentState(
      componentType: ComponentType,
      componentName: String,
      stateNames: Set[StateName],
      maxFrequency: Option[Int]
  ) extends WebSocketRequest

  case class Subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int] = None)                        extends WebSocketRequest
  case class SubscribeWithPattern(subsystem: Subsystem, maxFrequency: Option[Int], pattern: String = "*") extends WebSocketRequest
}
