package esw.sm.handler

import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import esw.sm.api.SequenceManagerApi
import esw.sm.api.codecs.SequenceManagerHttpCodec._
import esw.sm.api.protocol.SequenceManagerPostRequest
import esw.sm.api.protocol.SequenceManagerPostRequest._
import msocket.impl.post.{HttpPostHandler, ServerHttpCodecs}

class SequenceManagerPostHandler(sequenceManager: SequenceManagerApi)
    extends HttpPostHandler[SequenceManagerPostRequest]
    with ServerHttpCodecs {

  import sequenceManager._
  override def handle(request: SequenceManagerPostRequest): Route =
    request match {
      case Configure(obsMode)                   => complete(configure(obsMode))
      case GetRunningObsModes                   => complete(getRunningObsModes)
      case Cleanup(obsMode)                     => complete(cleanup(obsMode))
      case StartSequencer(subsystem, obsMode)   => complete(startSequencer(subsystem, obsMode))
      case RestartSequencer(subsystem, obsMode) => complete(restartSequencer(subsystem, obsMode))
      case ShutdownAllSequencers                => complete(shutdownAllSequencers())
      case ShutdownSequencer(subsystem, obsMode, shutdownSequenceComp) =>
        complete(shutdownSequencer(subsystem, obsMode, shutdownSequenceComp))
    }

}
