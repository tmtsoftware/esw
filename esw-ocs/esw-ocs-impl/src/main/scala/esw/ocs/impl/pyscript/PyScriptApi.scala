package esw.ocs.impl.pyscript

import akka.Done
import csw.params.commands.SequenceCommand
import csw.time.core.models.UTCTime
import esw.ocs.impl.script.ScriptApi

import scala.concurrent.Future
import org.graalvm.polyglot._

class PyScriptApi extends ScriptApi {
  override def execute(command: SequenceCommand): Future[Unit] = ???

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
