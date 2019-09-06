package esw.highlevel.dsl.javadsl

import java.time.Duration
import java.util.Optional
import java.util.concurrent.CompletionStage
import java.util.function.{Consumer, Supplier}

import akka.Done
import akka.actor.Cancellable
import csw.event.api.scaladsl.EventSubscription
import csw.params.core.generics.Parameter
import csw.params.events._
import esw.highlevel.dsl.EventServiceDsl
import esw.ocs.macros.StrandEc

import scala.compat.java8.OptionConverters.RichOptionalGeneric
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
import scala.jdk.DurationConverters.JavaDurationOps
import scala.jdk.FutureConverters.{CompletionStageOps, FutureOps}

trait JEventServiceDsl { self: EventServiceDsl =>

  def jSystemEvent(sourcePrefix: String, eventName: String, parameters: java.util.Set[Parameter[_]]): SystemEvent = {
    systemEvent(sourcePrefix, eventName, parameters.asScala.toSeq: _*)
  }

  def jObserveEvent(sourcePrefix: String, eventName: String, parameters: java.util.Set[Parameter[_]]): ObserveEvent = {
    observeEvent(sourcePrefix, eventName, parameters.asScala.toSeq: _*)
  }

  def jPublishEvent(event: Event): CompletionStage[Done] = {
    publishEvent(event).asJava
  }

  def jPublishEventAsync(
      every: Duration,
      eventGenerator: Supplier[CompletionStage[Optional[Event]]],
      strandEc: StrandEc
  ): Cancellable = {
    publishEventAsync(every.toScala)(eventGenerator.get().asScala.map(_.asScala)(strandEc.ec))(strandEc)
  }

  // fixme: return type
  def jOnEvent(eventKeys: java.util.Set[String], callback: Consumer[Event], strandEc: StrandEc): EventSubscription = {
    onEvent(eventKeys.asScala.toSeq: _*)(callback.accept)(strandEc)
  }

  def jGetEvent(eventKeys: java.util.Set[String], strandEc: StrandEc): CompletionStage[java.util.Set[Event]] = {
    getEvent(eventKeys.asScala.toSeq: _*).map(_.asJava)(strandEc.ec).asJava
  }

  private implicit def toEc(implicit strandEc: StrandEc): ExecutionContext = strandEc.ec
}
