package esw.backend.testkit.stubs

import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.scaladsl.Source
import csw.params.events.{Event, EventKey}
import csw.prefix.models.Subsystem
import esw.gateway.api.EventApi
import esw.ocs.testkit.utils.BaseTestSuite
import msocket.api.Subscription
import msocket.jvm.SourceExtension.WithSubscription

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class EventStubImpl(_actorSystem: ActorSystem[SpawnProtocol.Command]) extends EventApi with BaseTestSuite {

  var events: Set[Event] = Set.empty

  implicit def actorSystem: ActorSystem[SpawnProtocol.Command] = _actorSystem

  override def publish(event: Event): Future[Done] = {
    events += event
    Future.successful(Done)
  }

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] =
    Future.successful(events.filter(e => eventKeys.contains(e.eventKey)))

  override def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int]): Source[Event, Subscription] = {
    val futureStream = future(2.seconds, Source(events.filter(e => eventKeys.contains(e.eventKey))))
    Source
      .futureSource(futureStream)
      .mapMaterializedValue(_ => () => ())
      .withSubscription()
  }

  override def pSubscribe(subsystem: Subsystem, maxFrequency: Option[Int], pattern: String): Source[Event, Subscription] = {
    val futureStream = future(2.seconds, Source(events.filter(e => e.source.subsystem == subsystem)))
    Source
      .futureSource(futureStream)
      .mapMaterializedValue(_ => () => ())
      .withSubscription()
  }

}
