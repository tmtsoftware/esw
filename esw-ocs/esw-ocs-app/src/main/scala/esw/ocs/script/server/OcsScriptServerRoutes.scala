package esw.ocs.script.server

import csw.command.client.messages.DiagnosticDataMessage.DiagnosticMode
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import org.apache.pekko.http.scaladsl.model.StatusCodes.*
import org.apache.pekko.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import org.apache.pekko.http.scaladsl.server.{Directive0, Directives, ExceptionHandler, RejectionHandler, Route}
import org.apache.pekko.http.cors.scaladsl.CorsDirectives.*
import csw.logging.api.scaladsl.Logger
import csw.params.commands.SequenceCommand
import esw.ocs.impl.script.ScriptApi

import scala.concurrent.ExecutionContext

class OcsScriptServerRoutes(logger: Logger, script: ScriptApi, wiring: OcsScriptServerWiring)(implicit
    ec: ExecutionContext,
    sys: ActorSystem[_]
) extends Directives
    with ScriptJsonSupport {

  private val logRequest: HttpRequest => Unit = req => {
    logger.info(s"${req.method.value} ${req.uri.toString()}")
  }

  private val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ => logRequest))

  implicit def myExceptionHandler: ExceptionHandler =
    ExceptionHandler { case ex: Exception =>
      extractUri { uri =>
        println(s"Request to $uri could not be handled normally")
        ex.printStackTrace()
        complete(HttpResponse(InternalServerError, entity = "Internal error"))
      }
    }

  implicit def myRejectionHandler: RejectionHandler =
    RejectionHandler.default
      .mapRejectionResponse {
        case res @ HttpResponse(_, _, ent: HttpEntity.Strict, _) =>
          // since all pekko default rejection responses are Strict this will handle all rejections
          val message = ent.data.utf8String.replaceAll("\"", """\"""")

          // we copy the response in order to keep all headers and status code, wrapping the message as hand rolled JSON
          // you could the entity using your favourite marshalling library (e.g. spray json or anything else)
          res.withEntity(HttpEntity(ContentTypes.`application/json`, s"""{"rejection": "$message"}"""))

        case x => x // pass through all other types of responses
      }

  val route: Route = cors() {
    routeLogger {
      post {
        // Insert/update segment to M1 positions
        path("execute") {
          entity(as[SequenceCommand]) { sequenceCommand =>
            println(s"XXX script server: execute $sequenceCommand")
            complete(script.execute(sequenceCommand).map(_ => OK))
          }
        }
        ~ path("executeGoOnline") {
          println(s"XXX script server: executeGoOnline")
          complete(script.executeGoOnline().map(_ => OK))
        }
        ~ path("executeGoOffline") {
          println(s"XXX script server: executeGoOffline")
          complete(script.executeGoOffline().map(_ => OK))
        }
        ~ path("executeShutdown") {
          println(s"XXX script server: executeShutdown")
          complete(script.executeShutdown().map(_ => OK))
        }
        ~ path("executeAbort") {
          println(s"XXX script server: executeAbort")
          complete(script.executeAbort().map(_ => OK))
        }
        ~ path("executeNewSequenceHandler") {
          println(s"XXX script server: executeNewSequenceHandler")
          complete(script.executeNewSequenceHandler().map(_ => OK))
        }
        ~ path("executeStop") {
          println(s"XXX script server: executeStop")
          complete(script.executeStop().map(_ => OK))
        }
        ~ path("executeDiagnosticMode") {
          entity(as[DiagnosticMode]) { diagnosticMode =>
            println(s"XXX script server: executeDiagnosticMode: $diagnosticMode")
            complete(script.executeDiagnosticMode(diagnosticMode.startTime, diagnosticMode.hint).map(_ => OK))
          }
        }
        ~ path("executeOperationsMode") {
          println(s"XXX script server: executeOperationsMode")
          complete(script.executeOperationsMode().map(_ => OK))
        }
        ~ path("executeExceptionHandlers") {
          entity(as[String]) { msg =>
            println(s"XXX script server: executeExceptionHandlers")
            complete(script.executeExceptionHandlers(RuntimeException(msg)).map(_ => OK))
          }
        }
        ~ path("shutdownScript") {
          println(s"XXX script server: shutdownScript")
          wiring.locationService.unregister(wiring.httpConnection)
          script.shutdownScript()
          complete(OK)
        }
      }
    }
  }
}
