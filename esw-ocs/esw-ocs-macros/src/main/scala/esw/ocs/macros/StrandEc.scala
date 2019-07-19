package esw.ocs.macros

import java.util.concurrent.{Executors, ScheduledExecutorService}

import scala.concurrent.ExecutionContext

class StrandEc private (private[ocs] val executorService: ScheduledExecutorService) {
  val ec: ExecutionContext = ExecutionContext.fromExecutorService(executorService)
  def shutdown(): Unit     = executorService.shutdownNow()
}

object StrandEc {
  def apply(): StrandEc = new StrandEc(Executors.newSingleThreadScheduledExecutor())
}
