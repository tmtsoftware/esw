package esw.gateway.impl

import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{OnewayResponse, SubmitResponse, ValidateResponse}
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import esw.gateway.api.CommandApi
import esw.gateway.api.protocol.{CommandError, InvalidComponent, InvalidMaxFrequency}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

//fixme: Inject commandService from eswUtils later
class CommandImpl(commandService: (String, ComponentType) => Future[CommandService])(
    implicit ec: ExecutionContext,
    timeout: Timeout
) extends CommandApi {

  def submit(componentId: ComponentId, command: ControlCommand): Future[Either[InvalidComponent, SubmitResponse]] = {
    commandService(componentId.name, componentId.componentType)
      .flatMap(commandService => commandService.submit(command))
      .map(Right(_))
      .recover {
        case NonFatal(ex) => Left(InvalidComponent(ex.getMessage))
      }
  }

  def oneway(componentId: ComponentId, command: ControlCommand): Future[Either[InvalidComponent, OnewayResponse]] = {
    commandService(componentId.name, componentId.componentType)
      .flatMap(commandService => commandService.oneway(command))
      .map(Right(_))
      .recover {
        case NonFatal(ex) => Left(InvalidComponent(ex.getMessage))
      }
  }

  def validate(componentId: ComponentId, command: ControlCommand): Future[Either[InvalidComponent, ValidateResponse]] = {
    commandService(componentId.name, componentId.componentType)
      .flatMap(commandService => commandService.validate(command))
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

    def futureSource: Future[Source[CurrentState, Future[Option[InvalidComponent]]]] =
      commandService(componentId.name, componentId.componentType)
        .map(commandService => Utils.sourceWithNoError(commandService.subscribeCurrentState(stateNames)))
        .recover {
          case NonFatal(ex) => Utils.emptySourceWithError(InvalidComponent(ex.getMessage))
        }

    def currentStateSource: Source[CurrentState, Future[Option[InvalidComponent]]] = {
      Source.fromFutureSource(futureSource).mapMaterializedValue(_.flatten)
    }

    maxFrequency match {
      case Some(x) if x <= 0 => Utils.emptySourceWithError(InvalidMaxFrequency)
      case Some(frequency)   => currentStateSource.buffer(1, OverflowStrategy.dropHead).throttle(frequency, 1.second)
      case None              => currentStateSource
    }
  }
}
