package esw.ocs.app.route

import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.StandardRoute
import esw.ocs.api.SequencerCommandApi
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerCommandPostRequest
import esw.ocs.api.protocol.SequencerCommandPostRequest._
import msocket.api.MessageHandler
import msocket.impl.post.ServerHttpCodecs

class CommandPostHandlerImpl(sequencerCommand: SequencerCommandApi)
    extends MessageHandler[SequencerCommandPostRequest, StandardRoute]
    with SequencerHttpCodecs
    with ServerHttpCodecs {

  override def handle(request: SequencerCommandPostRequest): StandardRoute = request match {
    case LoadSequence(sequence)          => complete(sequencerCommand.loadSequence(sequence))
    case StartSequence                   => complete(sequencerCommand.startSequence())
    case SubmitSequence(sequence)        => complete(sequencerCommand.submit(sequence))
    case GoOnline                        => complete(sequencerCommand.goOnline())
    case GoOffline                       => complete(sequencerCommand.goOffline())
    case DiagnosticMode(startTime, hint) => complete(sequencerCommand.diagnosticMode(startTime, hint))
    case OperationsMode                  => complete(sequencerCommand.operationsMode())
  }
}
