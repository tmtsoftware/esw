package esw.gateway.server.routes.restless.messages

import csw.location.models.ComponentType
import csw.params.core.models.Id

trait WebSocketMsg

object WebSocketMsg {
  case class QueryCommandMsg(componentType: ComponentType, componentName: String, runId: Id) extends WebSocketMsg
}
