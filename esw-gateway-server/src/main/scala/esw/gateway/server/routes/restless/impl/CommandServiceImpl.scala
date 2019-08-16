package esw.gateway.server.routes.restless.impl

import akka.util.Timeout
import csw.params.commands.CommandResponse
import esw.gateway.server.routes.restless.api.CommandServiceApi
import esw.gateway.server.routes.restless.messages.CommandAction.{Oneway, Submit, Validate}
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg.InvalidComponent
import esw.gateway.server.routes.restless.messages.RequestMsg.CommandMsg
import esw.http.core.utils.CswContext

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.control.NonFatal

class CommandServiceImpl(cswCtx: CswContext) extends CommandServiceApi {

  import cswCtx._
  implicit val timeout: Timeout = Timeout(5.seconds)
  import actorRuntime.typedSystem.executionContext

  def process(commandMsg: CommandMsg): Future[Either[ErrorResponseMsg, CommandResponse]] = {
    import commandMsg._
    val commandServiceF = componentFactory.commandService(componentName, componentType)
    commandServiceF
      .flatMap { commandService =>
        action match {
          case Oneway   => commandService.oneway(command)
          case Submit   => commandService.submit(command)
          case Validate => commandService.validate(command)
        }
      }
      .map(Right(_))
      .recover {
        case NonFatal(ex) => Left(InvalidComponent(ex.getMessage))
      }
  }
}
