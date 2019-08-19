package esw.gateway.server.routes.restless.messages

import csw.location.models.ComponentType
import csw.params.core.models.{Id, Subsystem}
import csw.params.core.states.StateName

sealed trait WebSocketMsg

object WebSocketMsg {
  case class QueryCommandMsg(componentType: ComponentType, componentName: String, runId: Id) extends WebSocketMsg
  case class CurrentStateSubscriptionCommandMsg(
      componentType: ComponentType,
      componentName: String,
      stateNames: Set[StateName],
      maxFrequency: Option[Int]
  ) extends WebSocketMsg

  //fixme: Add codec for EventKey in CSW and use Set[EventKey]
  case class SubscribeEventMsg(eventKeys: Set[String], maxFrequency: Option[Int])                             extends WebSocketMsg
  case class PatternSubscribeEventMsg(subsystem: Subsystem, maxFrequency: Option[Int], pattern: String = "*") extends WebSocketMsg
}
