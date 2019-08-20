package esw.gateway.server.routes.restless.messages

import csw.location.models.ComponentType
import csw.params.core.models.{Id, Subsystem}
import csw.params.core.states.StateName
import csw.params.events.EventKey

sealed trait WebSocketMessage

object WebSocketMessage {
  case class QueryCommandMessage(componentType: ComponentType, componentName: String, runId: Id) extends WebSocketMessage
  case class CurrentStateSubscriptionCommandMessage(
      componentType: ComponentType,
      componentName: String,
      stateNames: Set[StateName],
      maxFrequency: Option[Int]
  ) extends WebSocketMessage

  //fixme: Add codec for EventKey in CSW and use Set[EventKey]
  case class SubscribeEventMessage(eventKeys: Set[EventKey], maxFrequency: Option[Int]) extends WebSocketMessage
  case class PatternSubscribeEventMessage(subsystem: Subsystem, maxFrequency: Option[Int], pattern: String = "*")
      extends WebSocketMessage
}
