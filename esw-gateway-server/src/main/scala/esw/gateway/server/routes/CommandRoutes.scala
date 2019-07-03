package esw.gateway.server.routes

import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.location.api.models.ComponentType
import csw.params.commands.ControlCommand
import csw.params.core.formats.JsonSupport
import csw.params.core.models.Id
import csw.params.core.states.StateName
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import esw.template.http.server.csw.utils.CswContext
import play.api.libs.json.Json

import scala.concurrent.duration.DurationLong

class CommandRoutes(cswCtx: CswContext) extends JsonSupport with PlayJsonSupport {
  import cswCtx._
  import actorRuntime._

  def commandRoutes(componentType: String): Route =
    pathPrefix("command") {
      pathPrefix(componentType / Segment) { componentName =>
        val commandServiceF = componentFactory.commandService(componentName, ComponentType.withName(componentType))

        onSuccess(commandServiceF) { commandService =>
          post {
            path("validate") {
              entity(as[ControlCommand]) { command =>
                complete(commandService.validate(command))
              }
            } ~
            path("submit") {
              entity(as[ControlCommand]) { command =>
                complete(commandService.submit(command))
              }
            } ~
            path("oneway") {
              entity(as[ControlCommand]) { command =>
                complete(commandService.oneway(command))
              }
            }
          } ~
          get {
            path(Segment) { runId =>
              val responseF = commandService.queryFinal(Id(runId))(Timeout(100.hours))
              // onSuccess directive is not used since we want to complete the request with an SSE stream even before the future completes.
              // This is because in case of long-running commands, future will complete after a long time and the http request will timeout if onSuccess is used.
              complete {
                Source
                  .fromFuture(responseF)
                  .map(response => ServerSentEvent(Json.toJson(response).toString()))
                  .keepAlive(1.second, () => ServerSentEvent.heartbeat)
              }
            } ~
            path("current-state" / "subscribe") {
              parameters("stateName".as[String].*) { stateNames =>
                complete {
                  commandService
                    .subscribeCurrentState(stateNames.map(StateName.apply).toSet)
                    .map(state => ServerSentEvent(Json.toJson(state).toString()))
                    .keepAlive(1.second, () => ServerSentEvent.heartbeat)
                }
              }
            }
          }
        }
      }
    }

  val route: Route = commandRoutes("hcd") ~ commandRoutes("assembly")
}
