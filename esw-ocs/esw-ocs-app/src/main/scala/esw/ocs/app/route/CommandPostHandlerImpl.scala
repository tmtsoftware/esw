package esw.ocs.app.route

import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.StandardRoute
import esw.ocs.api.SequencerCommandApi
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.{SequencerAdminPostRequest, SequencerCommandPostRequest}
import esw.ocs.api.protocol.SequencerAdminPostRequest._
import esw.ocs.api.protocol.SequencerCommandPostRequest.{LoadSequence, StartSequence, SubmitSequence}
import msocket.api.MessageHandler
import msocket.impl.post.ServerHttpCodecs

class CommandPostHandlerImpl(sequencerCommand: SequencerCommandApi)
    extends MessageHandler[SequencerCommandPostRequest, StandardRoute]
    with SequencerHttpCodecs
    with ServerHttpCodecs {

  override def handle(request: SequencerCommandPostRequest): StandardRoute = request match {
    case LoadSequence(sequence)   => complete(sequencerCommand.loadSequence(sequence))
    case StartSequence            => complete(sequencerCommand.startSequence())
    case SubmitSequence(sequence) => complete(sequencerCommand.submit(sequence))
  }
}
