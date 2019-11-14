package esw.ocs.app.route

import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.StandardRoute
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerAdminPostRequest
import esw.ocs.api.protocol.SequencerAdminPostRequest._
import msocket.impl.post.ServerHttpCodecs
import msocket.api.MessageHandler

class AdminPostHandlerImpl(sequencerAdmin: SequencerAdminApi)
    extends MessageHandler[SequencerAdminPostRequest, StandardRoute]
    with SequencerHttpCodecs
    with ServerHttpCodecs {

  override def handle(request: SequencerAdminPostRequest): StandardRoute = request match {
    case GetSequence               => complete(sequencerAdmin.getSequence)
    case IsAvailable               => complete(sequencerAdmin.isAvailable)
    case IsOnline                  => complete(sequencerAdmin.isOnline)
    case Pause                     => complete(sequencerAdmin.pause)
    case Resume                    => complete(sequencerAdmin.resume)
    case Reset                     => complete(sequencerAdmin.reset())
    case AbortSequence             => complete(sequencerAdmin.abortSequence())
    case Stop                      => complete(sequencerAdmin.stop())
    case Add(commands)             => complete(sequencerAdmin.add(commands))
    case Prepend(commands)         => complete(sequencerAdmin.prepend(commands))
    case Replace(id, commands)     => complete(sequencerAdmin.replace(id, commands))
    case InsertAfter(id, commands) => complete(sequencerAdmin.insertAfter(id, commands))
    case Delete(id)                => complete(sequencerAdmin.delete(id))
    case AddBreakpoint(id)         => complete(sequencerAdmin.addBreakpoint(id))
    case RemoveBreakpoint(id)      => complete(sequencerAdmin.removeBreakpoint(id))
  }
}
