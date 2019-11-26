package esw.ocs.dsl.script

import java.util.concurrent.CompletionStage
import java.util.function.Supplier

import akka.Done
import csw.params.commands.SequenceCommand
import csw.time.core.models.UTCTime

import scala.concurrent.Future

private[esw] class FSMScriptDsl(override val csw: CswServices, val strandEc: StrandEc) extends ScriptDsl(csw, strandEc) {
  protected var currentState: String = "UN_INITIALIZED"
  // fixme : should not be null.
  protected var currentStateDsl: ScriptDsl = _
  protected var stateMap                   = Map.empty[String, Supplier[ScriptDsl]]

  def become(nextState: String): Unit =
    if (currentState != nextState) {
      currentStateDsl = stateMap(nextState).get()
      currentState = nextState
    }

  def add(state: String, script: Supplier[ScriptDsl]): Unit = stateMap += (state -> script)

  override def execute(command: SequenceCommand): Future[Unit] = stateMap.get(currentState) match {
    case Some(_) => currentStateDsl.execute(command)
    case None    => Future.failed(new RuntimeException(s"Invalid state = $currentState"))
  }

  override def executeGoOnline(): Future[Done] = currentStateDsl.executeGoOnline().flatMap(_ => super.executeGoOnline())

  override def executeGoOffline(): Future[Done] = currentStateDsl.executeGoOffline().flatMap(_ => super.executeGoOffline())

  override def executeShutdown(): Future[Done] = currentStateDsl.executeShutdown().flatMap(_ => super.executeShutdown())

  override def executeAbort(): Future[Done] = currentStateDsl.executeAbort().flatMap(_ => super.executeAbort())

  override def executeStop(): Future[Done] = currentStateDsl.executeStop().flatMap(_ => super.executeStop())

  override def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done] =
    currentStateDsl.executeDiagnosticMode(startTime, hint).flatMap(_ => super.executeDiagnosticMode(startTime, hint))

  override def executeOperationsMode(): Future[Done] =
    currentStateDsl.executeOperationsMode().flatMap(_ => super.executeOperationsMode())

  override def executeExceptionHandlers(ex: Throwable): CompletionStage[Void] =
    currentStateDsl.executeExceptionHandlers(ex).thenAccept(_ => super.executeExceptionHandlers(ex))
}
