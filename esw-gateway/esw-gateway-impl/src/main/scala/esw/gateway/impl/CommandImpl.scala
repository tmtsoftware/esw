package esw.gateway.impl

import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import esw.gateway.api.CommandApi
import esw.gateway.api.messages.CommandAction.{Oneway, Submit, Validate}
import esw.gateway.api.messages.{CommandAction, CommandError, InvalidComponent, InvalidMaxFrequency}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

//fixme: Inject commandService from eswUtils later
class CommandImpl(commandService: (String, ComponentType) => Future[CommandService])(
    implicit ec: ExecutionContext,
    timeout: Timeout
) extends CommandApi {

  def process(
      componentId: ComponentId,
      command: ControlCommand,
      action: CommandAction
  ): Future[Either[InvalidComponent, CommandResponse]] = {
    commandService(componentId.name, componentId.componentType)
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

  def queryFinal(componentId: ComponentId, runId: Id): Future[Either[InvalidComponent, SubmitResponse]] = {
    commandService(componentId.name, componentId.componentType)
      .flatMap(_.queryFinal(runId)(Timeout(100.hours)))
      .map(Right(_))
      .recover {
        case NonFatal(ex) => Left(InvalidComponent(ex.getMessage))
      }
  }

  override def subscribeCurrentState(
      componentId: ComponentId,
      stateNames: Set[StateName],
      maxFrequency: Option[Int]
  ): Source[CurrentState, Future[Option[CommandError]]] = {

    val currentStateSource: Source[CurrentState, Future[Option[InvalidComponent]]] = {
      Source
        .fromFutureSource(
          commandService(componentId.name, componentId.componentType)
            .map(_.subscribeCurrentState(stateNames).mapMaterializedValue(_ => Future.successful(None)))
            .recover {
              case NonFatal(ex) => Utils.emptySourceWithError(InvalidComponent(ex.getMessage))
            }
        )
        .mapMaterializedValue(_.flatten)
    }

    maxFrequency match {
      case Some(x) if x <= 0 => Utils.emptySourceWithError(InvalidMaxFrequency())
      case Some(frequency)   => currentStateSource.buffer(1, OverflowStrategy.dropHead).throttle(frequency, 1.seconds)
      case None              => currentStateSource
    }
  }
}
