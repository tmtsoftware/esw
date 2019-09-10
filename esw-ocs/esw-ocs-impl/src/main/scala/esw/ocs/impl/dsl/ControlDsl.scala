package esw.ocs.impl.dsl

import akka.Done
import esw.ocs.impl.dsl.utils.FutureUtils
import esw.ocs.macros.{AsyncMacros, StrandEc}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros

trait ControlDsl {
  implicit lazy val strandEc: StrandEc               = StrandEc()
  protected implicit lazy val toEc: ExecutionContext = strandEc.ec
  private[ocs] def loopInterval: FiniteDuration

  protected final def par[T](fs: List[Future[T]]): Future[List[T]] = Future.sequence(fs)
  protected final def par[T](fs: Future[T]*): Future[List[T]]      = par(fs.toList)

  protected final def loop(block: => Future[StopIf]): Future[Done] = loop(loopInterval)(block)

  protected final def loop(minimumInterval: FiniteDuration)(block: => Future[StopIf]): Future[Done] =
    loopWithoutDelay(FutureUtils.delayedResult(minimumInterval max loopInterval)(block)(strandEc))

  protected final def stopIf(condition: Boolean): StopIf = StopIf(condition)

  protected implicit class RichF[T](t: Future[T]) {
    final def await: T = macro AsyncMacros.await
  }

  protected final def spawn[T](body: => T)(implicit strandEc: StrandEc): Future[T] = macro AsyncMacros.asyncStrand[T]

  private def loopWithoutDelay(block: => Future[StopIf]): Future[Done] =
    spawn {
      if (block.await.condition) Done else loopWithoutDelay(block).await
    }

}
