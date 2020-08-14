package esw.sm.handler

import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import esw.sm.api.SequenceManagerApi
import esw.sm.api.codecs.SequenceManagerHttpCodec._
import esw.sm.api.protocol.SequenceManagerPostRequest
import esw.sm.api.protocol.SequenceManagerPostRequest._
import esw.sm.auth.EswUserRolePolicy
import msocket.impl.post.{HttpPostHandler, ServerHttpCodecs}

class SequenceManagerPostHandler(sequenceManager: SequenceManagerApi, securityDirectives: SecurityDirectives)
    extends HttpPostHandler[SequenceManagerPostRequest]
    with ServerHttpCodecs {

  import sequenceManager._
  override def handle(request: SequenceManagerPostRequest): Route =
    request match {
      case GetRunningObsModes                   => complete(getRunningObsModes)
      case GetAgentStatus                       => complete(getAgentStatus)
      case Configure(obsMode)                   => sPost(complete(configure(obsMode)))
      case Provision(config)                    => sPost(complete(provision(config)))
      case StartSequencer(subsystem, obsMode)   => sPost(complete(startSequencer(subsystem, obsMode)))
      case RestartSequencer(subsystem, obsMode) => sPost(complete(restartSequencer(subsystem, obsMode)))

      // Shutdown sequencers
      case ShutdownSequencer(subsystem, obsMode)  => sPost(complete(shutdownSequencer(subsystem, obsMode)))
      case ShutdownSubsystemSequencers(subsystem) => sPost(complete(shutdownSubsystemSequencers(subsystem)))
      case ShutdownObsModeSequencers(obsMode)     => sPost(complete(shutdownObsModeSequencers(obsMode)))
      case ShutdownAllSequencers                  => sPost(complete(shutdownAllSequencers()))

      case ShutdownSequenceComponent(prefix) => sPost(complete(shutdownSequenceComponent(prefix)))
      case ShutdownAllSequenceComponents     => sPost(complete(shutdownAllSequenceComponents()))
    }

  def sPost(route: => Route): Route = securityDirectives.sPost(EswUserRolePolicy())(_ => route)
}
