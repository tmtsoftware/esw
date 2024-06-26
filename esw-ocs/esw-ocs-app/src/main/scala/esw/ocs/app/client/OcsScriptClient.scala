package esw.ocs.app.client

import csw.params.commands.SequenceCommand
import esw.ocs.impl.script.ScriptApi
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, Uri}
import cps.compat.FutureAsync.*
import csw.command.client.messages.DiagnosticDataMessage.DiagnosticMode
import csw.location.api.models.HttpLocation
import csw.params.core.formats.JsonSupport
import csw.time.core.models.UTCTime
import esw.ocs.script.server.ScriptJsonSupport
import org.apache.pekko.http.scaladsl.model.StatusCodes.OK

import java.net.URI
import scala.concurrent.{ExecutionContext, Future}

/**
 * HTTP Client for OcsScriptServer.
 */
class OcsScriptClient(loc: HttpLocation)(implicit
    typedSystem: ActorSystem[SpawnProtocol.Command],
    ec: ExecutionContext
) extends ScriptApi
    with ScriptJsonSupport {

  private val baseUri = loc.uri
  println(s"XXX script client: baseUri = $baseUri")

  def checkError(response: HttpResponse): Unit = {
    if (response.status != OK) throw new RuntimeException(s"Server responded with ${response.status}")
  }

  override def execute(command: SequenceCommand): Future[Unit] = async {
    println(s"XXX script client: execute $command")
    val uri  = Uri(s"${baseUri}execute")
    val json = JsonSupport.writeSequenceCommand(command).toString
    println(s"XXX script client: execute $command, json = $json")
    val entity  = HttpEntity(ContentTypes.`application/json`, json)
    val request = HttpRequest(HttpMethods.POST, uri = uri, entity = entity)
    checkError(await(Http().singleRequest(request)))
  }

  override def executeGoOnline(): Future[Done] = async {
    println(s"XXX script client: executeGoOnline")
    val uri     = Uri(s"${baseUri}executeGoOnline")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def executeGoOffline(): Future[Done] = async {
    println(s"XXX script client: executeGoOffline")
    val uri     = Uri(s"${baseUri}executeGoOffline")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    println(s"XXX script client: executeGoOffline: uri = $uri")
//    checkError(await(Http().singleRequest(request)))
    val xxx = await(Http().singleRequest(request))
    println(s"XXX script client: executeGoOffline: resp = $xxx")
    checkError(xxx)
    Done
  }

  override def executeShutdown(): Future[Done] = async {
    println(s"XXX script client: executeShutdown")
    val uri     = Uri(s"${baseUri}executeShutdown")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def executeAbort(): Future[Done] = async {
    println(s"XXX script client: executeAbort")
    val uri     = Uri(s"${baseUri}executeAbort")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def executeNewSequenceHandler(): Future[Done] = async {
    println(s"XXX script client: executeNewSequenceHandler")
    val uri     = Uri(s"${baseUri}executeNewSequenceHandler")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def executeStop(): Future[Done] = async {
    println(s"XXX script client: executeStop")
    val uri     = Uri(s"${baseUri}executeStop")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done] = async {
    println(s"XXX script client: executeDiagnosticMode")
    val uri     = Uri(s"${baseUri}executeDiagnosticMode")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    val json    = diagnosticModeFormat.write(DiagnosticMode(startTime, hint)).compactPrint
    val entity  = HttpEntity(ContentTypes.`application/json`, json)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def executeOperationsMode(): Future[Done] = async {
    println(s"XXX script client: executeOperationsMode")
    val uri     = Uri(s"${baseUri}executeOperationsMode")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def executeExceptionHandlers(ex: Throwable): Future[Done] = async {
    println(s"XXX script client: executeExceptionHandlers")
    val uri     = Uri(s"${baseUri}execute")
    val json    = ex.getMessage
    val entity  = HttpEntity(ContentTypes.`application/json`, json)
    val request = HttpRequest(HttpMethods.POST, uri = uri, entity = entity)
    checkError(await(Http().singleRequest(request)))
    Done
  }

  override def shutdownScript(): Unit = async {
    println(s"XXX script client: shutdownScript")
    val uri     = Uri(s"${baseUri}shutdownScript")
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    checkError(await(Http().singleRequest(request)))
  }
}
