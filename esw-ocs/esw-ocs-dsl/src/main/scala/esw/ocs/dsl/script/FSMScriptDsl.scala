package esw.ocs.dsl.script

import java.util.concurrent.CompletionStage
import java.util.function.Supplier

import akka.Done
import csw.params.commands.SequenceCommand
import csw.time.core.models.UTCTime

import scala.concurrent.Future

private[esw] class FSMScriptDsl(override val csw: CswServices, val strandEc: StrandEc) extends ScriptDsl(csw, strandEc) {
  protected var currentState                          = "UN_INITIALIZED"
  protected var maybeCurrentScript: Option[ScriptDsl] = None
  protected var stateMap                              = Map.empty[String, Supplier[ScriptDsl]]

  def become(nextState: String): Unit =
    if (currentState != nextState) {
      maybeCurrentScript = Some(getScript(nextState))
      currentState = nextState
    }

  def add(state: String, script: Supplier[ScriptDsl]): Unit = stateMap += (state -> script)

  override def execute(command: SequenceCommand): Future[Unit] = currentScript.execute(command)

  override def executeGoOnline(): Future[Done] = currentScript.executeGoOnline().flatMap(_ => super.executeGoOnline())

  override def executeGoOffline(): Future[Done] = currentScript.executeGoOffline().flatMap(_ => super.executeGoOffline())

  override def executeShutdown(): Future[Done] = currentScript.executeShutdown().flatMap(_ => super.executeShutdown())

  override def executeAbort(): Future[Done] = currentScript.executeAbort().flatMap(_ => super.executeAbort())

  override def executeStop(): Future[Done] = currentScript.executeStop().flatMap(_ => super.executeStop())

  override def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done] =
    currentScript.executeDiagnosticMode(startTime, hint).flatMap(_ => super.executeDiagnosticMode(startTime, hint))

  override def executeOperationsMode(): Future[Done] =
    currentScript.executeOperationsMode().flatMap(_ => super.executeOperationsMode())

  override def executeExceptionHandlers(ex: Throwable): CompletionStage[Void] =
    currentScript.executeExceptionHandlers(ex).thenAccept(_ => super.executeExceptionHandlers(ex))

  private def currentScript =
    maybeCurrentScript.getOrElse(throw new RuntimeException("Current script handler is not initialized"))

  private def getScript(state: String) =
    stateMap.getOrElse(state, throw new RuntimeException(s"No command handlers found for state: $state")).get()
}
