package esw.gateway.server.routes.restless.impl

import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.params.commands.CommandResponse
import csw.params.core.states.CurrentState
import esw.gateway.server.routes.restless.api.CommandServiceApi
import esw.gateway.server.routes.restless.messages.CommandAction.{Oneway, Submit, Validate}
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg.{InvalidComponent, InvalidMaxFrequency}
import esw.gateway.server.routes.restless.messages.HttpRequestMsg.CommandMsg
import esw.gateway.server.routes.restless.messages.WebSocketMsg.{CurrentStateSubscriptionCommandMsg, QueryCommandMsg}
import esw.gateway.server.routes.restless.utils.Utils.emptySourceWithError
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
    componentFactory
      .commandService(componentName, componentType)
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

  def queryFinal(queryCommandMsg: QueryCommandMsg): Future[Either[ErrorResponseMsg, CommandResponse]] = {
    import queryCommandMsg._
    componentFactory
      .commandService(componentName, componentType)
      .flatMap(_.queryFinal(runId)(Timeout(100.hours)))
      .map(Right(_))
      .recover {
        case NonFatal(ex) => Left(InvalidComponent(ex.getMessage))
      }
  }

  override def subscribeCurrentState(
      currentStateSubscriptionCommandMsg: CurrentStateSubscriptionCommandMsg
  ): Source[CurrentState, Future[Option[ErrorResponseMsg]]] = {
    import currentStateSubscriptionCommandMsg._

    def currentStateSource: Source[CurrentState, Future[Option[ErrorResponseMsg]]] = {
      Source
        .fromFutureSource(
          componentFactory
            .commandService(componentName, componentType)
            .map(_.subscribeCurrentState(stateNames).mapMaterializedValue(_ => Future.successful(None)))
            .recover {
              case NonFatal(ex) => emptySourceWithError(InvalidComponent(ex.getMessage))
            }
        )
        .mapMaterializedValue(_.flatten)
    }

    maxFrequency match {
      case Some(x) if x <= 0 =>
        emptySourceWithError(InvalidMaxFrequency())
      case Some(frequency) => currentStateSource.buffer(1, OverflowStrategy.dropHead).throttle(frequency, 1.seconds)
      case None            => currentStateSource
    }
  }
}
