package esw.ocs.script.server

import csw.params.commands.SequenceCommand
import csw.time.core.models.UTCTime
import esw.ocs.impl.script.{ScriptApi, ScriptContext}
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

/**
 * HTTP Client for OcsScriptServer.
 */
class OcsScriptClient(scriptClass: String)(implicit
    typedSystem: ActorSystem[SpawnProtocol.Command],
    ec: ExecutionContext
) extends ScriptApi {
  
  // XXX TODO Start and then use Location Service to find server

  override def execute(command: SequenceCommand): Future[Unit] = {
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(
      uri = "http://pekko.apache.org")
    )
  }

  override def executeGoOnline(): Future[Done] = ???

  override def executeGoOffline(): Future[Done] = ???

  override def executeShutdown(): Future[Done] = ???

  override def executeAbort(): Future[Done] = ???

  override def executeNewSequenceHandler(): Future[Done] = ???

  override def executeStop(): Future[Done] = ???

  override def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done] = ???

  override def executeOperationsMode(): Future[Done] = ???

  override def executeExceptionHandlers(ex: Throwable): Future[Done] = ???

  override def shutdownScript(): Unit = ???
}
