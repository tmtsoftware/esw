package esw.sm.handler

import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import esw.commons.auth.AuthPolicies
import esw.sm.api.SequenceManagerApi
import esw.sm.api.codecs.SequenceManagerServiceCodecs._
import esw.sm.api.protocol.SequenceManagerRequest
import esw.sm.api.protocol.SequenceManagerRequest._
import msocket.http.post.{HttpPostHandler, ServerHttpCodecs}

class SequenceManagerRequestHandler(sequenceManager: SequenceManagerApi, securityDirectives: SecurityDirectives)
    extends HttpPostHandler[SequenceManagerRequest]
    with ServerHttpCodecs {

  import sequenceManager._
  override def handle(request: SequenceManagerRequest): Route =
    request match {
      case GetObsModesDetails                   => complete(getObsModesDetails)
      case GetResources                         => complete(getResources)
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

  def sPost(route: => Route): Route = securityDirectives.sPost(AuthPolicies.eswUserRolePolicy)(_ => route)
}
