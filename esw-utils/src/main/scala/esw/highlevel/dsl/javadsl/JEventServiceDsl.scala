package esw.highlevel.dsl.javadsl

import java.time.Duration
import java.util
import java.util.Optional
import java.util.concurrent.{CompletableFuture, CompletionStage}
import java.util.function.Supplier

import akka.Done
import akka.actor.Cancellable
import csw.event.api.javadsl.{IEventPublisher, IEventSubscriber, IEventSubscription}
import csw.event.client.internal.commons.javawrappers.JEventService
import csw.params.core.generics.Parameter
import csw.params.events._
import esw.highlevel.dsl.EventServiceDsl

import scala.jdk.CollectionConverters._

trait JEventServiceDsl { self: EventServiceDsl =>
  lazy val jEventService                 = new JEventService(eventService)
  lazy val jPublisher: IEventPublisher   = jEventService.defaultPublisher
  lazy val jSubscriber: IEventSubscriber = jEventService.defaultSubscriber

  def jSystemEvent(sourcePrefix: String, eventName: String, parameters: java.util.Set[Parameter[_]]): SystemEvent =
    systemEvent(sourcePrefix, eventName, parameters.asScala.toSeq: _*)

  def jObserveEvent(sourcePrefix: String, eventName: String, parameters: java.util.Set[Parameter[_]]): ObserveEvent =
    observeEvent(sourcePrefix, eventName, parameters.asScala.toSeq: _*)

  def jPublishEvent(event: Event): CompletionStage[Done] =
    jPublisher.publish(event)

  def jPublishEventAsync(every: Duration, eventGenerator: Supplier[CompletableFuture[Optional[Event]]]): Cancellable =
    jPublisher.publishAsync(eventGenerator, every)

  def jOnEvent(eventKeys: util.Set[String], callback: Event => CompletableFuture[_]): IEventSubscription =
    jSubscriber.subscribeAsync(mapToEventKeys(eventKeys), callback)

  def jGetEvent(eventKeys: util.Set[String]): CompletableFuture[util.Set[Event]] =
    jSubscriber.get(mapToEventKeys(eventKeys))

  private def mapToEventKeys(eventKeys: util.Set[String]) = eventKeys.asScala.map(EventKey(_)).asJava
}
