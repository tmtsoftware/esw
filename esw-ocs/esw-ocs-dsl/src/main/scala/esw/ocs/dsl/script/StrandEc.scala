package esw.ocs.dsl.script

import java.util.concurrent.{Executors, ScheduledExecutorService}

import esw.ocs.impl.blockhound.{BlockHoundWiring, ScriptEcIntegration}

import scala.concurrent.ExecutionContext

class StrandEc private (private[esw] val executorService: ScheduledExecutorService) {
  val ec: ExecutionContext = ExecutionContext.fromExecutorService(executorService)
  def shutdown(): Unit     = executorService.shutdownNow()
}

object StrandEc {
  def apply(): StrandEc = {
    val threadName = "script-thread"
    BlockHoundWiring.addIntegration(new ScriptEcIntegration(threadName))
    new StrandEc(Executors.newSingleThreadScheduledExecutor((r: Runnable) => new Thread(r, threadName)))
  }
}
