package esw.ocs.framework.dsl

import akka.Done
import esw.ocs.framework.dsl.internal.FutureUtils
import esw.ocs.framework.executors.StrandEc

import scala.async.Async.{async, await}
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

trait ControlDsl {
  implicit lazy val strandEc: StrandEc               = StrandEc()
  protected implicit lazy val toEc: ExecutionContext = strandEc.ec
  // todo: should this come from conf file?
  private val loopInterval: FiniteDuration = 50.millis

  protected final def par[T](fs: Future[T]*): Future[List[T]] = Future.sequence(fs.toList)

  protected final def loop(block: => Future[StopIf]): Future[Done] = loop(loopInterval)(block)

  protected final def loop(minimumInterval: FiniteDuration)(block: => Future[StopIf]): Future[Done] =
    loopWithoutDelay(FutureUtils.delayedResult(minimumInterval max loopInterval)(block)(strandEc))

  protected final def stopIf(condition: Boolean): StopIf = StopIf(condition)

  // todo: use spawn when it gets ported to this repo
  private def loopWithoutDelay(block: => Future[StopIf]): Future[Done] =
    async {
      if (await(block).condition) Done else await(loopWithoutDelay(block))
    }

}
