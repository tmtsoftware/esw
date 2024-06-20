package esw.ocs.app.client

import csw.params.commands.SequenceCommand
import esw.ocs.impl.script.ScriptApi
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, Uri}
import cps.compat.FutureAsync.*
import csw.location.api.models.HttpLocation
import csw.params.core.formats.JsonSupport
import csw.time.core.models.UTCTime
import org.apache.pekko.http.scaladsl.model.StatusCodes.OK

import java.net.URI
import scala.concurrent.{ExecutionContext, Future}

/**
 * HTTP Client for OcsScriptServer.
 */
class OcsScriptClient(loc: HttpLocation)(implicit
    typedSystem: ActorSystem[SpawnProtocol.Command],
    ec: ExecutionContext
) extends ScriptApi {

  private val baseUri = loc.uri

  def checkError(response: HttpResponse): Unit = {
    if (response.status != OK) throw new RuntimeException(s"Server responded with ${response.status}")
  }

  override def execute(command: SequenceCommand): Future[Unit] = async {
    val uri     = Uri(s"$baseUri/execute")
    val json    = JsonSupport.writeSequenceCommand(command).toString
    val entity  = HttpEntity(ContentTypes.`application/json`, json)
    val request = HttpRequest(HttpMethods.POST, uri = uri, entity = entity)
    checkError(await(Http().singleRequest(request)))
  }

  override def executeGoOnline(): Future[Done] = async {
    val uri     = Uri(s"$baseUri/executeGoOnline")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def executeGoOffline(): Future[Done] = async {
    val uri     = Uri(s"$baseUri/executeGoOffline")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def executeShutdown(): Future[Done] = async {
    val uri     = Uri(s"$baseUri/executeShutdown")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def executeAbort(): Future[Done] = async {
    val uri     = Uri(s"$baseUri/executeAbort")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def executeNewSequenceHandler(): Future[Done] = async {
    val uri     = Uri(s"$baseUri/executeNewSequenceHandler")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def executeStop(): Future[Done] = async {
    val uri     = Uri(s"$baseUri/executeStop")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done] = async {
    val uri     = Uri(s"$baseUri/executeDiagnosticMode")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def executeOperationsMode(): Future[Done] = async {
    val uri     = Uri(s"$baseUri/executeOperationsMode")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def executeExceptionHandlers(ex: Throwable): Future[Done] = async {
    val uri     = Uri(s"$baseUri/execute")
    val json    = ex.getMessage
    val entity  = HttpEntity(ContentTypes.`application/json`, json)
    val request = HttpRequest(HttpMethods.POST, uri = uri, entity = entity)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def shutdownScript(): Unit = async {
    val uri     = Uri(s"$baseUri/shutdownScript")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
  }
}
