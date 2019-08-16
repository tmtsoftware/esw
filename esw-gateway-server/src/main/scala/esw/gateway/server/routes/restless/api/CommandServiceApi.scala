package esw.gateway.server.routes.restless.api

import csw.params.commands.CommandResponse
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg
import esw.gateway.server.routes.restless.messages.RequestMsg.CommandMsg
import esw.gateway.server.routes.restless.messages.WebSocketMsg.{CurrentStateSubscriptionCommandMsg, QueryCommandMsg}

import scala.concurrent.Future

trait CommandServiceApi {
  def process(commandMsg: CommandMsg): Future[Either[ErrorResponseMsg, CommandResponse]]
  def queryFinal(queryCommandMsg: QueryCommandMsg): Future[Either[ErrorResponseMsg, CommandResponse]]
  def subscribeCurrentState(currentStateSubscriptionCommandMsg: CurrentStateSubscriptionCommandMsg): Unit
}
