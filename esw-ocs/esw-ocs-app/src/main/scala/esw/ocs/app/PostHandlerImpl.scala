package esw.ocs.app

import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.StandardRoute
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.models.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.models.request.SequencerAdminPostRequest
import esw.ocs.api.models.request.SequencerAdminPostRequest.GetSequence
import mscoket.impl.HttpCodecs
import msocket.api.RequestHandler

class PostHandlerImpl(sequencerAdmin: SequencerAdminApi)
    extends RequestHandler[SequencerAdminPostRequest, StandardRoute]
    with SequencerAdminHttpCodecs
    with HttpCodecs {

  override def handle(request: SequencerAdminPostRequest): StandardRoute = request match {
    case GetSequence => complete(sequencerAdmin.getSequence)
  }
}
