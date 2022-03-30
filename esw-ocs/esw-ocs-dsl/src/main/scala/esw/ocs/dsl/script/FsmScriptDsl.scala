/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.script

import akka.Done
import csw.logging.api.javadsl.ILogger
import csw.params.commands.SequenceCommand
import csw.time.core.models.UTCTime
import esw.ocs.dsl.params.Params
import esw.ocs.impl.core.SequenceOperator

import scala.concurrent.Future

/**
 * Scala dsl for FSM(Finite State Machine) scripts in the sequencer scripts
 *
 * @param sequenceOperatorFactory - a factory/lamda which returns an instance of SequenceOperator
 * @param logger - A logger to log
 * @param strandEc - an StrandEc
 * @param shutdownTask - a Runnable to shut down the script
 * @param initialState - initial state of FSM from where the transition into the next state will begin
 */
private[esw] class FsmScriptDsl(
    sequenceOperatorFactory: () => SequenceOperator,
    logger: ILogger,
    strandEc: StrandEc,
    shutdownTask: Runnable,
    initialState: FsmScriptState
) extends ScriptDsl(sequenceOperatorFactory, logger, strandEc, shutdownTask) {

  def this(sequenceOperatorFactory: () => SequenceOperator, logger: ILogger, strandEc: StrandEc, shutdownTask: Runnable) =
    this(sequenceOperatorFactory, logger, strandEc, shutdownTask, FsmScriptState.init())

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
