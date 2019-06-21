package esw.gateway.server

import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.Route
import csw.params.commands.ControlCommand
import esw.template.http.server.CswContext

class Routes(cswCtx: CswContext) extends JsonSupportExt {
  import cswCtx._
  import actorRuntime._

  def route: Route =
    pathPrefix("assembly" / Segment) { assemblyName =>
      val commandServiceF = componentFactory.assemblyCommandService(assemblyName)

      post {
        path("submit") {
          entity(as[ControlCommand]) { command =>
            complete(commandServiceF.flatMap(_.submit(command)))
          }
        }
      }
    }
}
