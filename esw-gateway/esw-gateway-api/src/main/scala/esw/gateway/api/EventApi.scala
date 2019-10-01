package esw.gateway.api

import akka.Done
import akka.stream.scaladsl.Source
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}
import esw.gateway.api.protocol.{EventServerUnavailable, GetEventError}
import msocket.api.models.StreamStatus

import scala.concurrent.Future

trait EventApi {
  def publish(event: Event): Future[Either[EventServerUnavailable.type, Done]]
  def get(eventKeys: Set[EventKey]): Future[Either[GetEventError, Set[Event]]]
  def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int]): Source[Event, Future[StreamStatus]]

  def pSubscribe(
      subsystem: Subsystem,
      maxFrequency: Option[Int],
      pattern: String
  ): Source[Event, Future[StreamStatus]]

}
