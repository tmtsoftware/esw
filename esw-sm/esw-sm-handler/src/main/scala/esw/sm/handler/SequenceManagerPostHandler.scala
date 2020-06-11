package esw.sm.handler

import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.messages.SequenceManagerMsg.GetRunningObsModes
import esw.sm.api.codecs.SequenceManagerHttpCodec._
import esw.sm.api.protocol.SequenceManagerPostRequest
import msocket.impl.post.{HttpPostHandler, ServerHttpCodecs}

class SequenceManagerPostHandler(sequenceManager: SequenceManagerApi)
    extends HttpPostHandler[SequenceManagerPostRequest]
    with ServerHttpCodecs {

  override def handle(request: SequenceManagerPostRequest): Route =
    request match {
      case _: GetRunningObsModes => complete(sequenceManager.getRunningObsModes)
    }

}
