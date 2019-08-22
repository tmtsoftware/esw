package esw.gateway.api

import akka.stream.scaladsl.Source
import csw.location.models.ComponentType
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import esw.gateway.api.messages.{CommandAction, CommandError, InvalidComponent}

import scala.concurrent.Future

trait CommandServiceApi {

  def process(
      componentType: ComponentType,
      componentName: String,
      command: ControlCommand,
      action: CommandAction
  ): Future[Either[InvalidComponent, CommandResponse]]

  def queryFinal(componentType: ComponentType, componentName: String, runId: Id): Future[Either[InvalidComponent, SubmitResponse]]
  def subscribeCurrentState(
      componentType: ComponentType,
      componentName: String,
      stateNames: Set[StateName],
      maxFrequency: Option[Int]
  ): Source[CurrentState, Future[Option[CommandError]]]
}
