package esw.ocs.dsl.script

import akka.Done
import csw.params.commands.SequenceCommand
import csw.time.core.models.UTCTime
import esw.ocs.dsl.params.Params
import esw.ocs.impl.core.SequenceOperator

import scala.concurrent.Future

private[esw] class FsmScriptDsl(
    sequenceOperatorFactory: () => SequenceOperator,
    strandEc: StrandEc,
    shutdownTask: Runnable,
    initialState: FsmScriptState
) extends ScriptDsl(sequenceOperatorFactory, strandEc, shutdownTask) {

  def this(sequenceOperatorFactory: () => SequenceOperator, strandEc: StrandEc, shutdownTask: Runnable) =
    this(sequenceOperatorFactory, strandEc, shutdownTask, FsmScriptState.init())

  private var scriptState = initialState

  def become(nextState: String, params: Params): Unit = {
    scriptState = scriptState.transition(nextState, params)
    scriptState.currentScript // to trigger the evaluation of currentScript which is declared lazy.
  }

  def add(state: String, script: Params => ScriptDsl): Unit =
    scriptState = scriptState.add(state, script)

  override def execute(command: SequenceCommand): Future[Unit] =
    scriptState.currentScript.execute(command)

  override def executeGoOnline(): Future[Done] =
    scriptState.currentScript.executeGoOnline().flatMap(_ => super.executeGoOnline())

  override def executeGoOffline(): Future[Done] =
    scriptState.currentScript.executeGoOffline().flatMap(_ => super.executeGoOffline())

  override def executeShutdown(): Future[Done] =
    scriptState.currentScript.executeShutdown().flatMap(_ => super.executeShutdown())

  override def executeAbort(): Future[Done] =
    scriptState.currentScript.executeAbort().flatMap(_ => super.executeAbort())

  override def executeStop(): Future[Done] =
    scriptState.currentScript.executeStop().flatMap(_ => super.executeStop())

  override def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done] =
    scriptState.currentScript.executeDiagnosticMode(startTime, hint).flatMap(_ => super.executeDiagnosticMode(startTime, hint))

  override def executeOperationsMode(): Future[Done] =
    scriptState.currentScript.executeOperationsMode().flatMap(_ => super.executeOperationsMode())

  override def executeExceptionHandlers(ex: Throwable): Future[Done] =
    scriptState.currentScript.executeExceptionHandlers(ex).flatMap(_ => super.executeExceptionHandlers(ex))

  // for testing purpose
  private[script] def getState = scriptState
}
