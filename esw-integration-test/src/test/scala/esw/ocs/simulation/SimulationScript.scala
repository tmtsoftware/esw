package esw.ocs.simulation

import akka.Done
import csw.params.commands.SequenceCommand
import csw.time.core.models.UTCTime
import esw.ocs.impl.script.ScriptApi

import scala.concurrent.Future

object SimulationScript extends ScriptApi {
  private val done: Future[Done] = Future.successful(Done)

  override def execute(command: SequenceCommand): Future[Unit] = Future.successful(())

  override def executeNewSequenceHandler(): Future[Done] = done

  override def executeGoOnline(): Future[Done] = done

  override def executeGoOffline(): Future[Done] = done

  override def executeShutdown(): Future[Done] = done

  override def executeAbort(): Future[Done] = done

  override def executeStop(): Future[Done] = done

  override def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done] = done

  override def executeOperationsMode(): Future[Done] = done

  override def executeExceptionHandlers(ex: Throwable): Future[Done] = done

  override def shutdownScript(): Unit = {}
}
