package esw.ocs.admin.server

import akka.http.scaladsl.server.StandardRoute
import akka.util.Timeout
import esw.http.core.wiring.{HttpService, ServerWiring}
import esw.ocs.admin.api.{SequencerAdminApi, SequencerAdminPostRequest}
import esw.ocs.admin.impl.SequencerAdminImpl
import msocket.api.RequestHandler

import scala.concurrent.duration.DurationInt

class Wiring(_port: Option[Int]) {
  lazy val wiring = new ServerWiring(_port)
  import wiring._
  import cswCtx.{actorRuntime, _}
  import actorRuntime.typedSystem
  implicit val timeout: Timeout = 10.seconds

  private lazy val sequencerAdmin: SequencerAdminApi = new SequencerAdminImpl(locationService)

  lazy val postHandler: RequestHandler[SequencerAdminPostRequest, StandardRoute] =
    new SequencerPostHandlerImpl(sequencerAdmin)

  lazy val routes      = new Routes(postHandler, logger)
  lazy val httpService = new HttpService(logger, locationService, routes.route, settings, actorRuntime)
}
