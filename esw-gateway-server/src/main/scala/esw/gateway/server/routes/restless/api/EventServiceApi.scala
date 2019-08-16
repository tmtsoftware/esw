package esw.gateway.server.routes.restless.api

import akka.Done
import csw.params.events.Event
import esw.gateway.server.routes.restless.ErrorResponseMsg
import esw.gateway.server.routes.restless.RequestMsg.{GetEventMsg, PublishEventMsg}

import scala.concurrent.Future

trait EventServiceApi {

  def publish(publishEventMsg: PublishEventMsg): Future[Done]
  def get(getEventMsg: GetEventMsg): Future[Either[ErrorResponseMsg, Set[Event]]]
}
