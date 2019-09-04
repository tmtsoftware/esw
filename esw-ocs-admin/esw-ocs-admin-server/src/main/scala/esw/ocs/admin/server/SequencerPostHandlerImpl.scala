package esw.ocs.admin.server

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import esw.ocs.admin.api.SequencerAdminPostRequest.GetSequence
import esw.ocs.admin.api.{SequencerAdminApi, SequencerAdminHttpCodecs, SequencerAdminPostRequest}
import mscoket.impl.HttpCodecs
import msocket.api.RequestHandler

class SequencerPostHandlerImpl(sequencerAdminApi: SequencerAdminApi)
    extends RequestHandler[SequencerAdminPostRequest, StandardRoute]
    with SequencerAdminHttpCodecs
    with HttpCodecs {

  override def handle(request: SequencerAdminPostRequest): StandardRoute = request match {
    case GetSequence(sequencerName) => complete(sequencerAdminApi.getSequence(sequencerName))
  }
}
