package esw.gateway.server.routes.restless.api

import csw.params.commands.CommandResponse
import esw.gateway.server.routes.restless.ErrorResponseMsg
import esw.gateway.server.routes.restless.RequestMsg.CommandMsg

import scala.concurrent.Future

trait CommandServiceApi {
  def process(commandMsg: CommandMsg): Future[Either[ErrorResponseMsg, CommandResponse]]
}
