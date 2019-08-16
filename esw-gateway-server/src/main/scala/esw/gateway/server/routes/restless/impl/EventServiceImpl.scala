package esw.gateway.server.routes.restless.impl

import akka.Done
import csw.event.api.scaladsl.{EventPublisher, EventSubscriber}
import csw.params.events.{Event, EventKey}
import esw.gateway.server.routes.restless.api.EventServiceApi
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg.NoEventKeys
import esw.gateway.server.routes.restless.messages.RequestMsg.{GetEventMsg, PublishEventMsg}
import esw.http.core.utils.CswContext

import scala.concurrent.Future

class EventServiceImpl(cswCtx: CswContext) extends EventServiceApi {
  import cswCtx._
  import actorRuntime.typedSystem.executionContext

  lazy val subscriber: EventSubscriber = eventService.defaultSubscriber
  lazy val publisher: EventPublisher   = eventService.defaultPublisher

  override def publish(publishEventMsg: PublishEventMsg): Future[Done] = publisher.publish(publishEventMsg.event)

  override def get(getEventMsg: GetEventMsg): Future[Either[ErrorResponseMsg, Set[Event]]] = {
    import getEventMsg._
    if (keys.nonEmpty) subscriber.get(keys.toEventKeys).map(Right(_))
    else Future.successful(Left(NoEventKeys()))
  }

  implicit class RichEventKeys(keys: Iterable[String]) {
    def toEventKeys: Set[EventKey] = keys.map(EventKey(_)).toSet
  }

}
