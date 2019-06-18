package esw.ocs.framework.dsl

import akka.Done
import esw.ocs.framework.executors.StrandEc

import scala.async.Async.{async, await}
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

trait ControlDsl {
  implicit lazy val strandEc: StrandEc               = StrandEc()
  protected implicit lazy val toEc: ExecutionContext = strandEc.ec

  // todo: should this come from conf file?
  private val loopInterval: FiniteDuration = 50.millis

  def par[T](fs: Future[T]*): Future[List[T]] = Future.sequence(fs.toList)

  protected def loop(block: => Future[StopIf]): Future[Done] = loop(loopInterval)(block)

  protected def loop(minimumInterval: FiniteDuration)(block: => Future[StopIf]): Future[Done] =
    loopWithoutDelay(FutureUtils.delayedResult(minimumInterval max loopInterval)(block)(strandEc))

  // todo: use spawn when it gets ported to this repo
  private def loopWithoutDelay(block: => Future[StopIf]): Future[Done] =
    async {
      if (await(block).condition) Done else await(loopWithoutDelay(block))
    }

  protected def stopIf(condition: Boolean): StopIf = StopIf(condition)
}
