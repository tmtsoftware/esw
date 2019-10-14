package esw.ocs.dsl.script.utils

import java.time.Duration
import java.util.concurrent.{CompletionStage, TimeUnit}

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.SupervisorLockMessage
import csw.command.client.messages.SupervisorLockMessage.{Lock, Unlock}
import csw.command.client.models.framework.LockingResponse
import csw.location.models.ComponentType
import csw.params.core.models.Prefix
import esw.ocs.dsl.sequence_manager.LocationServiceUtil

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationLong, FiniteDuration}

class LockUnlockUtil(locationServiceUtil: LocationServiceUtil)(actorSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit val timeout: Timeout     = 5.seconds
  implicit val scheduler: Scheduler = actorSystem.scheduler
  implicit val ec: ExecutionContext = actorSystem.executionContext

  def jLock(
      componentName: String,
      componentType: ComponentType,
      prefix: Prefix,
      leaseDuration: Duration
  ): CompletionStage[LockingResponse] = processLockMessage(componentName, componentType) {
    Lock(prefix, _, FiniteDuration(leaseDuration.toNanos, TimeUnit.NANOSECONDS))
  }

  def jUnlock(
      componentName: String,
      componentType: ComponentType,
      prefix: Prefix
  ): CompletionStage[LockingResponse] = processLockMessage(componentName, componentType) { Unlock(prefix, _) }

  private def processLockMessage(componentName: String, componentType: ComponentType)(
      lockMsg: ActorRef[LockingResponse] => SupervisorLockMessage
  ): CompletionStage[LockingResponse] =
    locationServiceUtil
      .resolveComponentRef(componentName, componentType)
      .flatMap(_ ? lockMsg)
      .toJava
}
