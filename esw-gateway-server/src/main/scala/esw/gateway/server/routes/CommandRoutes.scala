package esw.gateway.server.routes

import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.Route
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.command.api.CurrentStateSubscription
import csw.location.client.HttpCodecs
import csw.location.models.ComponentType
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.formats.ParamCodecs
import csw.params.core.models.Id
import csw.params.core.states.{StateName, StateVariable}
import esw.http.core.commons.RichSourceExt.RichSource
import esw.http.core.commons.Utils._
import esw.http.core.utils.CswContext

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class CommandRoutes(cswCtx: CswContext) extends ParamCodecs with HttpCodecs {
  import cswCtx._

  implicit val timeout: Timeout = Timeout(5.seconds)

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
              val responseF: Future[CommandResponse] = commandService.queryFinal(Id(runId))(Timeout(100.hours))
              // onSuccess directive is not used since we want to complete the request with an SSE stream even before the future completes.
              // This is because in case of long-running commands, future will complete after a long time and the http request will timeout if onSuccess is used.
              complete(Source.fromFuture(responseF).toSSE)
            } ~
            path("current-state" / "subscribe") {
              parameters(("state-name".as[String].*, "max-frequency".as[Int].?)) { (stateNames, maxFrequency) =>
                validateFrequency(maxFrequency) {

                  val currentStateSource = commandService.subscribeCurrentState(stateNames.map(StateName.apply).toSet)
                  val stream: Source[StateVariable, CurrentStateSubscription] = maxFrequency match {
                    case Some(frequency) => currentStateSource.buffer(1, OverflowStrategy.dropHead).throttle(frequency, 1.seconds)
                    case None            => currentStateSource
                  }

                  complete(stream.toSSE)
                }
              }
            }
          }
        }
      }
    }

  val route: Route = commandRoutes(ComponentType.HCD.name) ~ commandRoutes(ComponentType.Assembly.name)
}
