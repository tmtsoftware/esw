package esw.highlevel.dsl

import akka.Done
import akka.actor.Cancellable
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber, EventSubscription}
import csw.params.core.generics.Parameter
import csw.params.core.models.Prefix
import csw.params.events._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

trait EventServiceDsl {
  private[esw] def eventService: EventService
  private lazy val publisher: EventPublisher   = eventService.defaultPublisher
  private lazy val subscriber: EventSubscriber = eventService.defaultSubscriber

  def systemEvent(sourcePrefix: String, eventName: String, parameters: Parameter[_]*): SystemEvent =
    SystemEvent(Prefix(sourcePrefix), EventName(eventName), parameters.toSet)

  def observeEvent(sourcePrefix: String, eventName: String, parameters: Parameter[_]*): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(eventName), parameters.toSet)

  def publishEvent(event: Event): Future[Done] = publisher.publish(event)

  def publishEvent(every: FiniteDuration)(eventGenerator: => Option[Event])(implicit ec: ExecutionContext): Cancellable =
    publisher.publishAsync(Future(eventGenerator), every)

  private val stringToEventKey = (x: String) => EventKey(x)
  def onEvent(eventKeys: String*)(callback: Event => Unit)(implicit ec: ExecutionContext): EventSubscription =
    subscriber.subscribeAsync(eventKeys.toSet.map(stringToEventKey(_)), event => Future(callback(event)))

  def getEvent(eventKeys: String*): Future[Set[Event]] = {
    val value: Set[EventKey] = eventKeys.toSet.map(stringToEventKey(_))
    subscriber.get(value)
  }
}
