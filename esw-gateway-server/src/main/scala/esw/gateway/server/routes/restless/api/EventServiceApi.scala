package esw.gateway.server.routes.restless.api

import akka.Done
import akka.stream.scaladsl.Source
import csw.params.events.Event
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg
import esw.gateway.server.routes.restless.messages.RequestMsg.{GetEventMsg, PublishEventMsg}
import esw.gateway.server.routes.restless.messages.WebSocketMsg.{PatternSubscribeEventMsg, SubscribeEventMsg}

import scala.concurrent.Future

trait EventServiceApi {

  def publish(publishEventMsg: PublishEventMsg): Future[Done]
  def get(getEventMsg: GetEventMsg): Future[Either[ErrorResponseMsg, Set[Event]]]
  def subscribe(subscribeEventMsg: SubscribeEventMsg): Source[Event, Future[Option[ErrorResponseMsg]]]
  def pSubscribe(patternSubscribeEventMsg: PatternSubscribeEventMsg): Source[Event, Future[Option[ErrorResponseMsg]]]
}
