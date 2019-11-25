package esw.ocs.app.route

import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerPostRequest
import esw.ocs.api.protocol.SequencerPostRequest._
import msocket.api.MessageHandler
import msocket.impl.post.ServerHttpCodecs

class SequencerPostHandlerImpl(adminApi: SequencerAdminApi)
    extends MessageHandler[SequencerPostRequest, Route]
    with SequencerHttpCodecs
    with ServerHttpCodecs {

  override def handle(request: SequencerPostRequest): Route = request match {
    // admin protocol
    case GetSequence               => complete(adminApi.getSequence)
    case IsAvailable               => complete(adminApi.isAvailable)
    case IsOnline                  => complete(adminApi.isOnline)
    case Pause                     => complete(adminApi.pause)
    case Resume                    => complete(adminApi.resume)
    case Reset                     => complete(adminApi.reset())
    case AbortSequence             => complete(adminApi.abortSequence())
    case Stop                      => complete(adminApi.stop())
    case Add(commands)             => complete(adminApi.add(commands))
    case Prepend(commands)         => complete(adminApi.prepend(commands))
    case Replace(id, commands)     => complete(adminApi.replace(id, commands))
    case InsertAfter(id, commands) => complete(adminApi.insertAfter(id, commands))
    case Delete(id)                => complete(adminApi.delete(id))
    case AddBreakpoint(id)         => complete(adminApi.addBreakpoint(id))
    case RemoveBreakpoint(id)      => complete(adminApi.removeBreakpoint(id))

    // command protocol
    case LoadSequence(sequence)          => complete(adminApi.loadSequence(sequence))
    case StartSequence                   => complete(adminApi.startSequence())
    case SubmitSequence(sequence)        => complete(adminApi.submit(sequence))
    case Query(runId)                    => complete(adminApi.query(runId))
    case GoOnline                        => complete(adminApi.goOnline())
    case GoOffline                       => complete(adminApi.goOffline())
    case DiagnosticMode(startTime, hint) => complete(adminApi.diagnosticMode(startTime, hint))
    case OperationsMode                  => complete(adminApi.operationsMode())
  }
}
