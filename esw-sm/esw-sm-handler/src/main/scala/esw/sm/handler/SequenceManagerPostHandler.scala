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
      case GetRunningObsModes                    => complete(getRunningObsModes)
      case GetAgentStatus                        => complete(getAgentStatus)
      case Configure(obsMode)                    => sPost(complete(configure(obsMode)))
      case StartSequencer(subsystem, obsMode)    => sPost(complete(startSequencer(subsystem, obsMode)))
      case RestartSequencer(subsystem, obsMode)  => sPost(complete(restartSequencer(subsystem, obsMode)))
      case ShutdownSequencers(policy)            => sPost(complete(shutdownSequencers(policy)))
      case SpawnSequenceComponent(machine, name) => sPost(complete(spawnSequenceComponent(machine, name)))
      case ShutdownSequenceComponents(policy)    => sPost(complete(shutdownSequenceComponents(policy)))
    }

  def sPost(route: => Route): Route = securityDirectives.sPost(EswUserRolePolicy())(_ => route)
}
