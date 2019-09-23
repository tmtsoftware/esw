package esw.dsl.script.utils

import java.util.concurrent.ScheduledExecutorService

import esw.ocs.macros.StrandEc

import scala.async.Async.{async, await}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}

private[esw] object FutureUtils {

  /**
   * returns a future which completes either
   * after minDelay or function Completion; whichever takes longer time
   *
   */
  def delayedResult[T](minDelay: FiniteDuration)(f: () => Future[T])(implicit strandEc: StrandEc): Future[T] = {
    async {
      val delayFuture = delay(minDelay, strandEc.executorService)
      val futureValue = f()
      await(delayFuture)
      await(futureValue)
    }(strandEc.ec)
  }

  private def delay(duration: FiniteDuration, executorService: ScheduledExecutorService): Future[Unit] = {
    val p = Promise[Unit]()
    executorService.schedule(() => p.success(()), duration.length, duration.unit)
    p.future
  }
}
