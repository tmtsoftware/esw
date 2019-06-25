package esw.gateway.server

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import esw.gateway.server.RouteExceptionHandlers.complete
import esw.template.http.server.CswContext
import play.api.libs.json.Json

import scala.concurrent.duration.DurationLong

class Routes(cswCtx: CswContext) extends JsonSupportExt {
  import cswCtx._
  import actorRuntime._

  def route: Route =
    handleExceptions(RouteExceptionHandlers.handlers) {

      pathPrefix("assembly" / Segment) { assemblyName =>
        val commandServiceF = componentFactory.assemblyCommandService(assemblyName)

        post {
          path("submit") {
            entity(as[ControlCommand]) { command =>
              complete(commandServiceF.flatMap(_.submit(command)))
            }
          } ~
          path("oneway") {
            entity(as[ControlCommand]) { command =>
              complete(commandServiceF.flatMap(_.oneway(command)))
            }
          }
        } ~
        get {
          path("queryFinal") {
            parameters(("runId", "timeout".as[Long])) { (runId, timeout) =>
              implicit val _timeout: Timeout = Timeout(timeout, TimeUnit.MILLISECONDS)
              val responseF = commandServiceF
                .flatMap(_.queryFinal(Id(runId)))
                .map(r => ServerSentEvent(Json.stringify(Json.toJson(r))))

              complete(
                Source
                  .fromFuture(responseF)
                  .keepAlive(1.second, () => ServerSentEvent.heartbeat)
              )
            }
          }
        }
      }
    }
}
