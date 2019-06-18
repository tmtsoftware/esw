package esw.ocs.framework.dsl

import java.util.concurrent.ScheduledExecutorService

import esw.ocs.framework.executors.StrandEc

import scala.async.Async._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}

object FutureUtils {

  /**
   * returns a future which completes either
   * after minDelay or function Completion; whichever takes longer time
   *
   */
  private[framework] def delay[T](minDelay: FiniteDuration)(f: => Future[T])(implicit strandEc: StrandEc): Future[T] = {
    async {
      val delayFuture = delay(minDelay, strandEc.executorService)
      val futureValue = f
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
