package esw.ocs.handler

import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import csw.aas.http.AuthorizationPolicy.CustomPolicy
import csw.aas.http.SecurityDirectives
import csw.command.client.auth.Roles
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.SequencerApi
import esw.ocs.api.codecs.SequencerHttpCodecs._
import esw.ocs.api.protocol.SequencerPostRequest
import esw.ocs.api.protocol.SequencerPostRequest._
import esw.ocs.auth.SubsystemUserRolePolicy
import msocket.impl.post.{HttpPostHandler, ServerHttpCodecs}

class SequencerPostHandler(
    sequencerApi: SequencerApi,
    securityDirectives: SecurityDirectives,
    destinationPrefix: Option[Prefix] = None
) extends HttpPostHandler[SequencerPostRequest]
    with ServerHttpCodecs {

  import sequencerApi._

  override def handle(request: SequencerPostRequest): Route = request match {
    case LoadSequence(sequence) => sPost()(complete(loadSequence(sequence)))
    case StartSequence          => sPost()(complete(startSequence()))

    case GetSequence               => complete(getSequence)
    case GetSequenceComponent      => complete(getSequenceComponent)
    case Add(commands)             => sPost()(complete(add(commands)))
    case Prepend(commands)         => sPost()(complete(prepend(commands)))
    case Replace(id, commands)     => sPost()(complete(replace(id, commands)))
    case InsertAfter(id, commands) => sPost()(complete(insertAfter(id, commands)))
    case Delete(id)                => sPost()(complete(delete(id)))
    case AddBreakpoint(id)         => sPost()(complete(addBreakpoint(id)))
    case RemoveBreakpoint(id)      => sPost()(complete(removeBreakpoint(id)))
    case Reset                     => sPost()(complete(reset()))
    case Pause                     => sPost()(complete(pause))
    case Resume                    => sPost()(complete(resume))

    case IsAvailable   => complete(isAvailable)
    case IsOnline      => complete(isOnline)
    case GoOnline      => sPost()(complete(goOnline()))
    case GoOffline     => sPost()(complete(goOffline()))
    case AbortSequence => sPost()(complete(abortSequence()))
    case Stop          => sPost()(complete(stop()))

    case DiagnosticMode(startTime, hint) => sPost()(complete(diagnosticMode(startTime, hint)))
    case OperationsMode                  => sPost()(complete(operationsMode()))

    // Sequencer Command Protocol
    case Submit(sequence) => sPost()(complete(submit(sequence)))
    case Query(runId)     => complete(query(runId))
  }

  private def sPost()(route: => Route) = {
    destinationPrefix match {
      case Some(prefix) => securityDirectives.sPost(SubsystemUserRolePolicy(prefix.subsystem))(_ => route)
      case None         => route // auth is disabled in this case
    }
  }
}
