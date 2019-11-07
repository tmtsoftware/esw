package esw.ocs.dsl.script.utils

import java.time.Duration
import java.util.concurrent.{CompletionStage, TimeUnit}

import akka.Done
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler, SpawnProtocol}
import akka.stream.scaladsl.Sink
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.Timeout
import csw.command.client.messages.SupervisorLockMessage.{Lock, Unlock}
import csw.command.client.models.framework.LockingResponse
import csw.command.client.models.framework.LockingResponse._
import csw.location.models.ComponentType
import csw.params.core.models.Prefix
import esw.ocs.dsl.sequence_manager.LocationServiceUtil

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.CompletionStageOps

class LockUnlockUtil(locationServiceUtil: LocationServiceUtil)(actorSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit val timeout: Timeout             = 5.seconds
  private implicit val scheduler: Scheduler = actorSystem.scheduler
  private implicit val ec: ExecutionContext = actorSystem.executionContext
  private implicit val mat: Materializer    = Materializer(actorSystem)

  def lock(componentName: String, componentType: ComponentType, prefix: Prefix, leaseDuration: Duration)(
      callback: LockingResponse => CompletionStage[Void]
  ): CompletionStage[Done] = {
    val leaseFiniteDuration  = FiniteDuration(leaseDuration.toNanos, TimeUnit.NANOSECONDS)
    val eventualComponentRef = locationServiceUtil.resolveComponentRef(componentName, componentType)

    eventualComponentRef.flatMap { compRef =>
      actorSource
        .mapMaterializedValue { lockResponseReplyTo =>
          compRef ! Lock(prefix, lockResponseReplyTo, leaseFiniteDuration)
        }
        .mapAsync(1)(executeCallback(callback))
        .takeWhile(isNotFinalLockResponse)
        .runWith(Sink.ignore)
    }.toJava
  }

  def unlock(componentName: String, componentType: ComponentType, prefix: Prefix): CompletionStage[LockingResponse] =
    locationServiceUtil
      .resolveComponentRef(componentName, componentType)
      .flatMap(_ ? (Unlock(prefix, _: ActorRef[LockingResponse])))
      .toJava

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

  private def executeCallback[T](cb: T => CompletionStage[Void]): T => Future[T] = { res =>
    cb(res).asScala.map(_ => res)
  }
}
