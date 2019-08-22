package esw.gateway.impl

import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.location.models.ComponentType
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import esw.gateway.api.GatewayApi
import esw.gateway.api.messages.CommandAction.{Oneway, Submit, Validate}
import esw.gateway.api.messages.{CommandAction, CommandError, InvalidComponent, InvalidMaxFrequency}
import esw.gateway.impl.syntax.SourceExtension

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal

trait CommandGatewayImpl extends GatewayApi {
  implicit val timeout: Timeout = Timeout(5.seconds)

  import cswContext._
  import actorRuntime.ec
  def process(
      componentType: ComponentType,
      componentName: String,
      command: ControlCommand,
      action: CommandAction
  ): Future[Either[InvalidComponent, CommandResponse]] = {
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

  def queryFinal(
      componentType: ComponentType,
      componentName: String,
      runId: Id
  ): Future[Either[InvalidComponent, SubmitResponse]] = {
    componentFactory
      .commandService(componentName, componentType)
      .flatMap(_.queryFinal(runId)(Timeout(100.hours)))
      .map(Right(_))
      .recover {
        case NonFatal(ex) => Left(InvalidComponent(ex.getMessage))
      }
  }

  override def subscribeCurrentState(
      componentType: ComponentType,
      componentName: String,
      stateNames: Set[StateName],
      maxFrequency: Option[Int]
  ): Source[CurrentState, Future[Option[CommandError]]] = {

    val currentStateSource: Source[CurrentState, Future[Option[InvalidComponent]]] = {
      Source
        .fromFutureSource(
          componentFactory
            .commandService(componentName, componentType)
            .map(_.subscribeCurrentState(stateNames).mapMaterializedValue(_ => Future.successful(None)))
            .recover {
              case NonFatal(ex) => SourceExtension.emptyWithError(InvalidComponent(ex.getMessage))
            }
        )
        .mapMaterializedValue(_.flatten)
    }

    maxFrequency match {
      case Some(x) if x <= 0 => SourceExtension.emptyWithError(InvalidMaxFrequency())
      case Some(frequency)   => currentStateSource.buffer(1, OverflowStrategy.dropHead).throttle(frequency, 1.seconds)
      case None              => currentStateSource
    }
  }
}
