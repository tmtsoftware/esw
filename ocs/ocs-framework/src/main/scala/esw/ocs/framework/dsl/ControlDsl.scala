package esw.ocs.framework.dsl

import akka.Done
import esw.ocs.framework.executors.StrandEc

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

trait ControlDsl {
  implicit lazy val strandEc: StrandEc             = StrandEc()
  private implicit lazy val toEc: ExecutionContext = strandEc.ec
  private val loopInterval: FiniteDuration         = 50.millis

  def par[T](fs: Future[T]*): Future[List[T]] = Future.sequence(fs.toList)

  protected def loop(block: => Future[Boolean]): Future[Done] = loop(loopInterval)(block)

  protected def loop(minimumInterval: FiniteDuration)(block: => Future[Boolean]): Future[Done] =
    loopWithoutDelay(FutureUtils.delay[Boolean](minimumInterval max loopInterval)(block)(strandEc))

  private def loopWithoutDelay(block: => Future[Boolean]): Future[Done] = ???
  //spawn {
//    if (block.await) Done else loopWithoutDelay(block).await
//  }

  protected def stopWhen(condition: Boolean): Boolean = condition
}
