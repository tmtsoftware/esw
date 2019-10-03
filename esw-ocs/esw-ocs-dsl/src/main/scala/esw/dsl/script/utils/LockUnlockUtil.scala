package esw.dsl.script.utils

import java.time.Duration
import java.util.concurrent.{CompletionStage, TimeUnit}

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.SupervisorLockMessage.{Lock, Unlock}
import csw.command.client.models.framework.LockingResponse
import csw.location.models.ComponentType
import csw.params.core.models.Prefix
import esw.dsl.sequence_manager.LocationServiceUtil

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

class LockUnlockUtil(locationServiceUtil: LocationServiceUtil)(actorSystem: ActorSystem[SpawnProtocol]) {

  implicit val timeout: Timeout     = 5.seconds
  implicit val scheduler: Scheduler = actorSystem.scheduler
  implicit val ec: ExecutionContext = actorSystem.executionContext

  def jLock(
      componentName: String,
      componentType: ComponentType,
      prefix: Prefix,
      leaseDuration: Duration
  ): CompletionStage[LockingResponse] = {
    val eventualResponse: Future[LockingResponse] = locationServiceUtil
      .resolveComponentRef(componentName, componentType)
      .flatMap { actorRef =>
        actorRef ? (Lock(prefix, _, FiniteDuration(leaseDuration.toNanos, TimeUnit.NANOSECONDS)))
      }
    eventualResponse.toJava
  }

  def jUnlock(
      componentName: String,
      componentType: ComponentType,
      prefix: Prefix
  ): CompletionStage[LockingResponse] = {
    val eventualResponse: Future[LockingResponse] = locationServiceUtil
      .resolveComponentRef(componentName, componentType)
      .flatMap { actorRef =>
        actorRef ? (Unlock(prefix, _))
      }
    eventualResponse.toJava
  }
}
