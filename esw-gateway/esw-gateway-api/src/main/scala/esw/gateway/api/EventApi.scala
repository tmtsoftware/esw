package esw.gateway.api

import akka.Done
import akka.stream.scaladsl.Source
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}
import msocket.api.Subscription

import scala.concurrent.Future

trait EventApi {
  def publish(event: Event): Future[Done]
  def get(eventKeys: Set[EventKey]): Future[Set[Event]]
  def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int] = None): Source[Event, Subscription]
  def pSubscribe(subsystem: Subsystem, maxFrequency: Option[Int] = None, pattern: String = "*"): Source[Event, Subscription]
}
