package esw.ocs.dsl.script.utils

import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Scheduler, SpawnProtocol}
import org.apache.pekko.stream.scaladsl.{Keep, Sink}
import org.apache.pekko.stream.typed.scaladsl.ActorSource
import org.apache.pekko.stream.{Materializer, OverflowStrategy}
import org.apache.pekko.util.Timeout
import csw.command.client.messages.ComponentMessage
import csw.command.client.messages.SupervisorLockMessage.{Lock, Unlock}
import csw.command.client.models.framework.LockingResponse
import csw.command.client.models.framework.LockingResponse.*
import csw.prefix.models.Prefix

import java.time.Duration
import java.util.concurrent.{CompletionStage, TimeUnit}
import scala.jdk.FutureConverters.*
import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.jdk.FutureConverters.CompletionStageOps
import scala.util.Success

/**
 * A Util class mainly written to send Lock/Unlock command to a particular HCD/Assembly
 *
 * @param source - represents the prefix of component that is acquiring lock
 * @param actorSystem - a Pekko ActorSystem
 */
class LockUnlockUtil(val source: Prefix)(actorSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit val timeout: Timeout             = 5.seconds
  private implicit val scheduler: Scheduler = actorSystem.scheduler
  private implicit val ec: ExecutionContext = actorSystem.executionContext
  private implicit val mat: Materializer    = Materializer(actorSystem)

  /**
   * Sends an Lock message to the given typed actor ref of the component
   *
   * @param componentRef - typed actor ref of the component whom to send the Lock message
   * @param leaseDuration - represents the lease duration of lock acquired
   * @param onLockAboutToExpire - a callback which is to be triggered when lock is about to expire
   * @param onLockExpired - a callback which is to be triggered when lock gets expired
   * @return the [[csw.command.client.models.framework.LockingResponse]] as CompletionStage value
   */
  def lock(componentRef: ActorRef[ComponentMessage], leaseDuration: Duration)(
      onLockAboutToExpire: () => CompletionStage[Void],
      onLockExpired: () => CompletionStage[Void]
  ): CompletionStage[LockingResponse] = {
    val leaseFiniteDuration = FiniteDuration(leaseDuration.toNanos, TimeUnit.NANOSECONDS)

    val firstLockResponse: Promise[LockingResponse] = Promise()

    actorSource
      .mapMaterializedValue { lockResponseReplyTo =>
        componentRef ! Lock(source, lockResponseReplyTo, leaseFiniteDuration)
        firstLockResponse.future
      }
      .mapAsync(1) { lockResponse =>
        firstLockResponse.tryComplete(Success(lockResponse))
        executeCallbacks(onLockAboutToExpire, onLockExpired)(lockResponse)
      }
      .takeWhile(isNotFinalLockResponse)
      .toMat(Sink.ignore)(Keep.left)
      .run()
      .asJava
  }

  /**
   * Sends an Unlock message to the given typed actor ref of the component
   *
   * @param componentRef - typed actor ref of the component whom to send the Lock message
   * @return the [[csw.command.client.models.framework.LockingResponse]] as CompletionStage value
   */
  def unlock(componentRef: ActorRef[ComponentMessage]): CompletionStage[LockingResponse] =
    (componentRef ? (Unlock(source, _: ActorRef[LockingResponse]))).asJava

  private def actorSource =
    ActorSource
      .actorRef[LockingResponse](
        completionMatcher = PartialFunction.empty,
        failureMatcher = PartialFunction.empty,
        bufferSize = 256,
        overflowStrategy = OverflowStrategy.dropHead
      )

  private def isFinalLockResponse: LockingResponse => Boolean = {
    case LockAcquired        => false
    case LockExpiringShortly => false
    case _                   => true
  }

  private def isNotFinalLockResponse: LockingResponse => Boolean = !isFinalLockResponse(_)

  private def executeCallbacks(
      onLockAboutToExpire: () => CompletionStage[Void],
      onLockExpired: () => CompletionStage[Void]
  ): LockingResponse => Future[LockingResponse] = {
    case res @ LockExpired         => onLockExpired().asScala.map(_ => res)
    case res @ LockExpiringShortly => onLockAboutToExpire().asScala.map(_ => res)
    case res                       => Future.successful(res)
  }
}
