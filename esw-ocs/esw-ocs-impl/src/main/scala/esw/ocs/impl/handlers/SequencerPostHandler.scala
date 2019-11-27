package esw.ocs.impl.handlers

import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import esw.ocs.api.SequencerApi
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerPostRequest
import esw.ocs.api.protocol.SequencerPostRequest._
import msocket.api.MessageHandler
import msocket.impl.post.ServerHttpCodecs

// fixme: Having handlers in ocs-impl adds dependency on akka-http, consider making another module
class SequencerPostHandler(sequencerApi: SequencerApi)
    extends MessageHandler[SequencerPostRequest, Route]
    with SequencerHttpCodecs
    with ServerHttpCodecs {
  import sequencerApi._

  override def handle(request: SequencerPostRequest): Route = request match {
    // admin protocol
    case GetSequence               => complete(getSequence)
    case IsAvailable               => complete(isAvailable)
    case IsOnline                  => complete(isOnline)
    case Pause                     => complete(pause)
    case Resume                    => complete(resume)
    case Reset                     => complete(reset())
    case AbortSequence             => complete(abortSequence())
    case Stop                      => complete(stop())
    case Add(commands)             => complete(add(commands))
    case Prepend(commands)         => complete(prepend(commands))
    case Replace(id, commands)     => complete(replace(id, commands))
    case InsertAfter(id, commands) => complete(insertAfter(id, commands))
    case Delete(id)                => complete(delete(id))
    case AddBreakpoint(id)         => complete(addBreakpoint(id))
    case RemoveBreakpoint(id)      => complete(removeBreakpoint(id))

    // command protocol
    case LoadSequence(sequence)          => complete(loadSequence(sequence))
    case StartSequence                   => complete(startSequence())
    case SubmitSequence(sequence)        => complete(submit(sequence))
    case Query(runId)                    => complete(query(runId))
    case GoOnline                        => complete(goOnline())
    case GoOffline                       => complete(goOffline())
    case DiagnosticMode(startTime, hint) => complete(diagnosticMode(startTime, hint))
    case OperationsMode                  => complete(operationsMode())
  }
}
