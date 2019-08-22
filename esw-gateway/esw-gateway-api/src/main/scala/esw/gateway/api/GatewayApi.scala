package esw.gateway.api

import akka.Done
import akka.stream.scaladsl.Source
import csw.alarm.models.AlarmSeverity
import csw.location.models.ComponentType
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.{Id, Subsystem}
import csw.params.core.states.{CurrentState, StateName}
import csw.params.events.{Event, EventKey}
import esw.gateway.api.messages.{
  CommandAction,
  CommandError,
  EmptyEventKeys,
  EventError,
  InvalidComponent,
  InvalidMaxFrequency,
  SetAlarmSeverityFailure
}
import esw.http.core.utils.CswContext

import scala.concurrent.Future

trait GatewayApi {
  val cswContext: CswContext

  //*****************AlarmService***********************
  def setSeverity(
      subsystem: Subsystem,
      componentName: String,
      alarmName: String,
      severity: AlarmSeverity
  ): Future[Either[SetAlarmSeverityFailure, Done]]

  //*****************CommandService***********************
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

  ////*****************EventService***********************
  def publish(event: Event): Future[Done]
  def get(eventKeys: Set[EventKey]): Future[Either[EmptyEventKeys, Set[Event]]]
  def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int]): Source[Event, Future[Option[EventError]]]
  def pSubscribe(
      subsystem: Subsystem,
      maxFrequency: Option[Int],
      pattern: String = "*"
  ): Source[Event, Future[Option[InvalidMaxFrequency]]]
}
