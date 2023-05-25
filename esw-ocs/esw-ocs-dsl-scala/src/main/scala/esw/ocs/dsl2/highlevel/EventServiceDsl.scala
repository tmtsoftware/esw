package esw.ocs.dsl2.highlevel

import akka.Done
import akka.actor.Cancellable
import csw.event.api.scaladsl.{EventPublisher, EventSubscriber, EventSubscription}
import csw.params.core.generics.Parameter
import csw.params.events as e
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix

import async.Async.*
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

class EventServiceDsl(eventPublisher: EventPublisher, eventSubscriber: EventSubscriber)(using ExecutionContext) {
  def EventKey(prefix: String, eventName: String): EventKey =
    e.EventKey(Prefix(prefix), EventName(eventName))

  export e.EventKey

  def SystemEvent(sourcePrefix: String, eventName: String, parameters: Parameter[_]*): SystemEvent =
    e.SystemEvent(Prefix(sourcePrefix), EventName(eventName)).madd(parameters*)

  inline def publishEvent(event: Event): Done = await(eventPublisher.publish(event))

  inline def publishEvent(every: FiniteDuration)(inline eventGenerator: () => Event): Cancellable =
    eventPublisher.publishAsync(async(Option(eventGenerator())), every)

  inline def onEvent(eventKeys: String*)(inline callback: Event => Unit): EventSubscription = {
    eventSubscriber.subscribeAsync(
      eventKeys.map(e.EventKey.apply).toSet,
      event => async(callback(event))
    )
  }

  extension (eventSubscription: EventSubscription)
    inline def cancel(): Done =
      await(eventSubscription.ready())
      await(eventSubscription.unsubscribe())

  inline def getEvent(eventKeys: String*): Set[Event] =
    await(eventSubscriber.get(eventKeys.map(e.EventKey.apply).toSet))

  inline def getEvent(eventKey: String): Event =
    await(eventSubscriber.get(EventKey(eventKey)))

}
