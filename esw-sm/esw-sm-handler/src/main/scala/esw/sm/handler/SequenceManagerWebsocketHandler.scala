package esw.sm.handler

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import esw.sm.api.SequenceManagerApi
import esw.sm.api.codecs.SequenceManagerHttpCodec
import esw.sm.api.codecs.SequenceManagerHttpCodec._
import esw.sm.api.protocol.SequenceManagerWebsocketRequest
import esw.sm.api.protocol.SequenceManagerWebsocketRequest._
import msocket.api.ContentType
import msocket.impl.ws.WebsocketHandler

class SequenceManagerWebsocketHandler(sequenceManager: SequenceManagerApi, contentType: ContentType)
    extends WebsocketHandler[SequenceManagerWebsocketRequest](contentType)
    with SequenceManagerHttpCodec {
  import sequenceManager._

  override def handle(request: SequenceManagerWebsocketRequest): Source[Message, NotUsed] =
    request match {
      case Configure(obsMode)                   => stream(configure(obsMode))
      case Cleanup(obsMode)                     => stream(cleanup(obsMode))
      case StartSequencer(subsytem, obsMode)    => stream(startSequencer(subsytem, obsMode))
      case ShutdownSequencer(subsytem, obsMode) => stream(shutdownSequencer(subsytem, obsMode))
      case RestartSequencer(subsytem, obsMode)  => stream(restartSequencer(subsytem, obsMode))
    }
}
