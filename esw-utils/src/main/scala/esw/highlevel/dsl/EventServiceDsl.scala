package esw.highlevel.dsl

import akka.Done
import akka.actor.Cancellable
import csw.event.api.exceptions.PublishFailure
import csw.event.api.scaladsl.{EventPublisher, EventService}
import csw.params.events.Event
import csw.time.core.models.{TMTTime, UTCTime}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class EventServiceDsl(eventService: EventService) {

  private lazy val publisher: EventPublisher = eventService.defaultPublisher

  def publish(event: Event): Future[Done] = publisher.publish(event)

  def publish(
      every: FiniteDuration,
      startTime: TMTTime = UTCTime.now()
  )(
      eventGenerator: => Option[Event],
      onError: PublishFailure => Unit = _ => ()
  )(implicit ec: ExecutionContext): Cancellable = publisher.publishAsync(Future(eventGenerator), startTime, every, onError)
}
