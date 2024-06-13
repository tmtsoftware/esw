package esw.ocs.script.server

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import org.apache.pekko.http.scaladsl.model.StatusCodes.*
import org.apache.pekko.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import org.apache.pekko.http.scaladsl.server.{Directive0, Directives, ExceptionHandler, RejectionHandler, Route}
import org.apache.pekko.http.cors.scaladsl.CorsDirectives.*
import csw.logging.api.scaladsl.Logger
import csw.params.commands.SequenceCommand
import csw.params.core.formats.JsonSupport
import esw.ocs.impl.script.ScriptApi

import scala.concurrent.{ExecutionContext, Future}
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.*

trait ScriptJsonSupport extends SprayJsonSupport with DefaultJsonProtocol with JsonSupport {
  implicit object sequenceCommandFormat extends RootJsonFormat[SequenceCommand] {
    def write(obj: SequenceCommand): JsString = JsString(writeSequenceCommand(obj).toString)

    def read(json: JsValue): SequenceCommand = json match {
      case JsString(s) => readSequenceCommand(play.api.libs.json.Json.parse(s))
      case _           => throw DeserializationException("SequenceCommand expected")
    }
  }
}

class OcsScriptServerRoutes(logger: Logger, script: ScriptApi)(implicit
    ec: ExecutionContext,
    sys: ActorSystem[_]
) extends Directives
    with ScriptJsonSupport {

  val logRequest: HttpRequest => Unit = req => {
    logger.info(s"${req.method.value} ${req.uri.toString()}")
  }

  val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ => logRequest))

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
            complete(script.execute(sequenceCommand).map(_ => OK))
          }
        }
      }
    }
  }
}
