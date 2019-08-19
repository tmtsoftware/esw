package esw.gateway.server.routes.restless.api

import akka.stream.scaladsl.Source
import csw.params.commands.CommandResponse
import csw.params.core.states.CurrentState
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg
import esw.gateway.server.routes.restless.messages.HttpRequestMsg.CommandMsg
import esw.gateway.server.routes.restless.messages.WebSocketMsg.{CurrentStateSubscriptionCommandMsg, QueryCommandMsg}

import scala.concurrent.Future

trait CommandServiceApi {
  def process(commandMsg: CommandMsg): Future[Either[ErrorResponseMsg, CommandResponse]]
  def queryFinal(queryCommandMsg: QueryCommandMsg): Future[Either[ErrorResponseMsg, CommandResponse]]
  def subscribeCurrentState(
      currentStateSubscriptionCommandMsg: CurrentStateSubscriptionCommandMsg
  ): Source[CurrentState, Future[Option[ErrorResponseMsg]]]
}
