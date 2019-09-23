package esw.dsl.script.services.javadsl

import java.time.Duration
import java.util
import java.util.Optional
import java.util.concurrent.{CompletableFuture, CompletionStage}
import java.util.function.Supplier

import akka.Done
import akka.actor.Cancellable
import csw.event.api.javadsl.{IEventPublisher, IEventSubscriber, IEventSubscription}
import csw.event.api.scaladsl.EventService
import csw.event.client.internal.commons.javawrappers.JEventService
import csw.params.core.generics.Parameter
import csw.params.core.models.Prefix
import csw.params.events._

import scala.jdk.CollectionConverters._

trait JEventServiceDsl {

  private[esw] val _eventService: EventService

  lazy val eventService                 = new JEventService(_eventService)
  lazy val publisher: IEventPublisher   = eventService.defaultPublisher
  lazy val subscriber: IEventSubscriber = eventService.defaultSubscriber

  def systemEvent(sourcePrefix: String, eventName: String, parameters: util.Set[Parameter[_]]): SystemEvent =
    SystemEvent(Prefix(sourcePrefix), EventName(eventName), parameters.asScala.toSet)

  def observeEvent(sourcePrefix: String, eventName: String, parameters: util.Set[Parameter[_]]): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(eventName), parameters.asScala.toSet)

  def publishEvent(event: Event): CompletionStage[Done] =
    publisher.publish(event)

  def publishEvent(every: Duration, eventGenerator: Supplier[CompletableFuture[Optional[Event]]]): Cancellable =
    publisher.publishAsync(eventGenerator, every)

  def onEvent(eventKeys: util.Set[String], callback: Event => CompletableFuture[_]): IEventSubscription =
    subscriber.subscribeAsync(eventKeys.asScala.map(EventKey(_)).asJava, callback)

  def getEvent(eventKeys: util.Set[String]): CompletableFuture[util.Set[Event]] =
    subscriber.get(eventKeys.asScala.map(EventKey(_)).toSet.asJava)
}
