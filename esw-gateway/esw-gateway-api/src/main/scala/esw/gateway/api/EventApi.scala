package esw.gateway.api

import akka.Done
import akka.stream.scaladsl.Source
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}
import esw.gateway.api.messages.{EmptyEventKeys, EventError, InvalidMaxFrequency}

import scala.concurrent.Future

trait EventApi {
  def publish(event: Event): Future[Done]
  def get(eventKeys: Set[EventKey]): Future[Either[EmptyEventKeys, Set[Event]]]
  def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int]): Source[Event, Future[Option[EventError]]]

  def pSubscribe(
      subsystem: Subsystem,
      maxFrequency: Option[Int],
      pattern: String
  ): Source[Event, Future[Option[InvalidMaxFrequency]]]

}
