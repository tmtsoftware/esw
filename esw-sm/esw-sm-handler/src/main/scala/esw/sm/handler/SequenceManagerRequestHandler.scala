package esw.sm.handler

import org.apache.pekko.http.scaladsl.server.Directives.complete
import org.apache.pekko.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import esw.commons.auth.AuthPolicies
import esw.sm.api.SequenceManagerApi
import esw.sm.api.codecs.SequenceManagerServiceCodecs.*
import esw.sm.api.protocol.SequenceManagerRequest
import esw.sm.api.protocol.SequenceManagerRequest.*
import msocket.http.post.{HttpPostHandler, ServerHttpCodecs}

/**
 * This is the Http(POST) route handler written using msocket apis for Sequence Manager.
 *
 * @param sequenceManager - an instance of sequencerApi of the sequence manager
 * @param securityDirectives - security directive to deal with auth
 */
class SequenceManagerRequestHandler(sequenceManager: SequenceManagerApi, securityDirectives: SecurityDirectives)
    extends HttpPostHandler[SequenceManagerRequest]
    with ServerHttpCodecs {

  import sequenceManager.*
  override def handle(request: SequenceManagerRequest): Route =
    request match {
      case GetObsModesDetails                              => complete(getObsModesDetails)
      case GetResources                                    => complete(getResources)
      case Configure(obsMode)                              => sPost(complete(configure(obsMode)))
      case Provision(config)                               => sPost(complete(provision(config)))
      case StartSequencer(subsystem, obsMode, variation)   => sPost(complete(startSequencer(subsystem, obsMode, variation)))
      case RestartSequencer(subsystem, obsMode, variation) => sPost(complete(restartSequencer(subsystem, obsMode, variation)))

      // Shutdown sequencers
      case ShutdownSequencer(subsystem, obsMode, variation) => sPost(complete(shutdownSequencer(subsystem, obsMode, variation)))
      case ShutdownSubsystemSequencers(subsystem)           => sPost(complete(shutdownSubsystemSequencers(subsystem)))
      case ShutdownObsModeSequencers(obsMode)               => sPost(complete(shutdownObsModeSequencers(obsMode)))
      case ShutdownAllSequencers                            => sPost(complete(shutdownAllSequencers()))

      case ShutdownSequenceComponent(prefix) => sPost(complete(shutdownSequenceComponent(prefix)))
      case ShutdownAllSequenceComponents     => sPost(complete(shutdownAllSequenceComponents()))

    }

  def sPost(route: => Route): Route = securityDirectives.sPost(AuthPolicies.eswUserRolePolicy)(_ => route)
}
