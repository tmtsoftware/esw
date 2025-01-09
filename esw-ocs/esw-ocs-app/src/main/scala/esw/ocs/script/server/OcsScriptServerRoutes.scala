package esw.ocs.script.server

import csw.command.client.messages.DiagnosticDataMessage.DiagnosticMode
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import org.apache.pekko.http.scaladsl.model.StatusCodes.*
import org.apache.pekko.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import org.apache.pekko.http.scaladsl.server.{Directive0, Directives, ExceptionHandler, Route}
import csw.logging.api.scaladsl.Logger
import csw.params.commands.SequenceCommand
import esw.ocs.impl.script.ScriptApi

import scala.concurrent.ExecutionContext

private[ocs] class OcsScriptServerRoutes(logger: Logger, script: ScriptApi, wiring: OcsScriptServerWiring)(implicit
    ec: ExecutionContext,
    sys: ActorSystem[?]
) extends Directives
    with ScriptJsonSupport {

  private val logRequest: HttpRequest => Unit = req => {
    logger.info(s"${req.method.value} ${req.uri.toString()}")
  }

  private val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ => logRequest))

  implicit def myExceptionHandler: ExceptionHandler =
    ExceptionHandler { case ex: Exception =>
      extractUri { uri =>
        val msg = if (ex.getCause != null) ex.getCause.getMessage else ex.getMessage
        complete(HttpResponse(InternalServerError, entity = msg))
      }
    }

  val route: Route = {
    routeLogger {
      handleExceptions(myExceptionHandler) {
        post {
          path("execute") {
            entity(as[SequenceCommand]) { sequenceCommand =>
              complete(script.execute(sequenceCommand).map(_ => OK))
            }
          }
          ~ path("executeGoOnline") {
            complete(script.executeGoOnline().map(_ => OK))
          }
          ~ path("executeGoOffline") {
            complete(script.executeGoOffline().map(_ => OK))
          }
          ~ path("executeShutdown") {
            // Note: Throws RejectedExecutionException somewhere
            val f = for {
              _ <- script.executeShutdown()
//              _ <- wiring.shutdownHttpService()
            } yield OK
            complete(f)
          }
          ~ path("executeAbort") {
            complete(script.executeAbort().map(_ => OK))
          }
          ~ path("executeNewSequenceHandler") {
            complete(script.executeNewSequenceHandler().map(_ => OK))
          }
          ~ path("executeStop") {
            complete(script.executeStop().map(_ => OK))
          }
          ~ path("executeDiagnosticMode") {
            entity(as[DiagnosticMode]) { diagnosticMode =>
              complete(script.executeDiagnosticMode(diagnosticMode.startTime, diagnosticMode.hint).map(_ => OK))
            }
          }
          ~ path("executeOperationsMode") {
            complete(script.executeOperationsMode().map(_ => OK))
          }
          ~ path("executeExceptionHandlers") {
            entity(as[String]) { msg =>
              complete(script.executeExceptionHandlers(RuntimeException(msg)).map(_ => OK))
            }
          }
          ~ path("shutdownScript") {
            script.shutdownScript()
            complete(wiring.shutdownHttpService().map(_ => OK))
          }
        }
      }
    }
  }
}
