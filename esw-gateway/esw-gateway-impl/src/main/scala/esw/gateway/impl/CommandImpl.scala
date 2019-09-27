package esw.gateway.impl

import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.location.models.ComponentId
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
class CommandImpl(commandService: ComponentId => Future[CommandService])(
    implicit ec: ExecutionContext,
    timeout: Timeout
) extends CommandApi {

  def submit(componentId: ComponentId, command: ControlCommand): Future[Either[InvalidComponent, SubmitResponse]] =
    process(componentId, _.submit(command))

  def oneway(componentId: ComponentId, command: ControlCommand): Future[Either[InvalidComponent, OnewayResponse]] =
    process(componentId, _.oneway(command))

  def validate(componentId: ComponentId, command: ControlCommand): Future[Either[InvalidComponent, ValidateResponse]] = {
    process(componentId, _.validate(command))
  }

  def queryFinal(componentId: ComponentId, runId: Id): Future[Either[InvalidComponent, SubmitResponse]] = {
    process(componentId, _.queryFinal(runId)(Timeout(100.hours)))
  }

  def process[T](componentId: ComponentId, action: CommandService => Future[T]): Future[Either[InvalidComponent, T]] = {
    commandService(componentId)
      .flatMap(action)
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
      commandService(componentId)
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
