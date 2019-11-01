package esw.gateway.api

import akka.stream.scaladsl.Source
import csw.location.models.ComponentId
import csw.params.commands.CommandResponse.{OnewayResponse, SubmitResponse, ValidateResponse}
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import esw.gateway.api.protocol.InvalidComponent
import msocket.api.models.StreamStatus

import scala.concurrent.Future

trait CommandApi {
  def submit(componentId: ComponentId, command: ControlCommand): Future[Either[InvalidComponent, SubmitResponse]]
  def oneway(componentId: ComponentId, command: ControlCommand): Future[Either[InvalidComponent, OnewayResponse]]
  def validate(componentId: ComponentId, command: ControlCommand): Future[Either[InvalidComponent, ValidateResponse]]
  def queryFinal(componentId: ComponentId, runId: Id): Future[Either[InvalidComponent, SubmitResponse]]

  def subscribeCurrentState(
      componentId: ComponentId,
      stateNames: Set[StateName] = Set.empty,
      maxFrequency: Option[Int] = None
  ): Source[CurrentState, Future[StreamStatus]]
}
