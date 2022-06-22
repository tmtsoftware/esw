package esw.ocs.impl.pyscript

import akka.Done
import csw.params.commands.SequenceCommand
import csw.time.core.models.UTCTime
import esw.ocs.impl.script.ScriptApi

import scala.concurrent.{ExecutionContext, Future}
import org.graalvm.polyglot.*

trait PyScriptApi {
  def execute(command: SequenceCommand): Unit

  def executeGoOnline(): Unit = {
    println("XXX default impl of executeGoOnline()")
  }

  def executeGoOffline(): Unit

  def executeShutdown(): Unit

  def executeAbort(): Unit

  def executeNewSequenceHandler(): Unit

  def executeStop(): Unit

  def executeDiagnosticMode(startTime: String, hint: String): Unit

  def executeOperationsMode(): Unit

  def executeExceptionHandlers(ex: String): Unit

  def shutdownScript(): Unit

}

class PyScriptApiWrapper(api: PyScriptApi)(implicit ec: ExecutionContext) extends ScriptApi {
  override def execute(command: SequenceCommand): Future[Unit] = Future(api.execute(command))

  override def executeGoOnline(): Future[Done] = Future(api.executeGoOnline()).map(_ => Done)

  override def executeGoOffline(): Future[Done] = Future(api.executeGoOffline()).map(_ => Done)

  override def executeShutdown(): Future[Done] = Future(api.executeShutdown()).map(_ => Done)

  override def executeAbort(): Future[Done] = Future(api.executeAbort()).map(_ => Done)

  override def executeNewSequenceHandler(): Future[Done] = Future(api.executeNewSequenceHandler()).map(_ => Done)

  override def executeStop(): Future[Done] = Future(api.executeStop()).map(_ => Done)

  override def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done] =
    Future(api.executeDiagnosticMode(startTime.toString, hint)).map(_ => Done)

  override def executeOperationsMode(): Future[Done] = Future(api.executeOperationsMode()).map(_ => Done)

  override def executeExceptionHandlers(ex: Throwable): Future[Done] =
    Future(api.executeExceptionHandlers(ex.toString)).map(_ => Done)

  override def shutdownScript(): Unit = Future(api.shutdownScript())
}
