/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.script

import esw.ocs.impl.blockhound.{BlockHoundWiring, ScriptEcIntegration}

import java.util.concurrent.{Executors, ScheduledExecutorService}
import scala.concurrent.ExecutionContext

//This class is to create a execution context
class StrandEc private (private[esw] val executorService: ScheduledExecutorService) {
  val ec: ExecutionContext = ExecutionContext.fromExecutorService(executorService)
  def shutdown(): Unit     = executorService.shutdownNow()
}

object StrandEc {
  /*
   * This factory creates an Execution context with a single Thread
   * (this is created for sequencer-scripts since they will be running on a single thread)
   */
  def apply(): StrandEc = {
    val threadName = "script-thread"
    BlockHoundWiring.addIntegration(new ScriptEcIntegration(threadName))
    new StrandEc(Executors.newSingleThreadScheduledExecutor((r: Runnable) => new Thread(r, threadName)))
  }
}
