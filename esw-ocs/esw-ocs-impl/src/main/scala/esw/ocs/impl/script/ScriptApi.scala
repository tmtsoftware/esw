package esw.ocs.impl.script

import java.util.concurrent.CompletionStage

import akka.Done
import csw.params.commands.SequenceCommand
import csw.time.core.models.UTCTime

import scala.concurrent.Future

trait ScriptApi {
  def execute(command: SequenceCommand): Future[Unit]
  def executeGoOnline(): Future[Done]
  def executeGoOffline(): Future[Done]
  def executeShutdown(): Future[Done]
  def executeAbort(): Future[Done]
  def executeStop(): Future[Done]
  def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done]
  def executeOperationsMode(): Future[Done]
  def executeExceptionHandlers(ex: Throwable): CompletionStage[Void]
  def shutdownScript(): Unit
}
