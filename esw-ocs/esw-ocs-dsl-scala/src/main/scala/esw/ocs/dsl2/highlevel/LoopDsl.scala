package esw.ocs.dsl2.highlevel

import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl2.highlevel.LoopDsl.{ExitLoop, loopInterval}

import java.util.concurrent.ScheduledExecutorService
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{ExecutionContext, Future, Promise}
import async.Async.*
import scala.annotation.tailrec

class LoopDsl(strandEc: StrandEc) {
  given ExecutionContext                 = strandEc.ec
  def break(): Nothing                   = throw ExitLoop()
  def stopWhen(condition: Boolean): Unit = if (condition) break()

  inline def loop(inline block: Unit): Unit                        = loop(loopInterval)(block)
  inline def loop(minInterval: Duration)(inline block: Unit): Unit = await(loopAsync(minInterval)(block))
  inline def waitFor(condition: Boolean): Unit                     = loop(stopWhen(condition))

  inline def loopAsync(inline block: Unit): Future[Unit]                        = loopAsync(loopInterval)(block)
  inline def loopAsync(minInterval: Duration)(inline block: Unit): Future[Unit] = loop0(minInterval)(async(block))

  private def loop0(minInterval: Duration)(block: => Future[Unit]): Future[Unit] = async {
    try
      await(delayedResult(minInterval.max(loopInterval))(block))
      await(loop0(minInterval)(block))
    catch case ex: ExitLoop => ()
  }

  private def delayedResult[T](minDelay: Duration)(block: => Future[T]): Future[T] = async {
    delay(minDelay)
    await(block)
  }

  inline def delay(duration: Duration): Unit = await(delayAsync(duration))

  private def delayAsync(duration: Duration): Future[Unit] =
    val p: Promise[Unit] = Promise()

    strandEc.executorService.schedule(
      () => p.trySuccess(()),
      duration.length,
      duration.unit
    )

    p.future
  end delayAsync

}

object LoopDsl {
  private val loopInterval: Duration = 50.milliseconds
  private class ExitLoop() extends RuntimeException
}
