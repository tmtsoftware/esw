package esw.dsl.script.utils

import java.time.Duration
import java.util.concurrent.{CompletionStage, ExecutionException, TimeUnit}

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.command.client.messages.ComponentMessage
import csw.command.client.messages.SupervisorLockMessage.{Lock, Unlock}
import csw.command.client.models.framework.LockingResponse
import csw.location.models.ComponentType
import csw.params.core.models.Prefix
import esw.dsl.sequence_manager.LocationServiceUtil
import esw.ocs.api.BaseTestSuite

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class LockUnlockUtilTest extends BaseTestSuite {
  "jLock" must {
    implicit val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test")
    val locationServiceUtil: LocationServiceUtil         = mock[LocationServiceUtil]
    val componentName                                    = "test-assembly"
    val componentType                                    = ComponentType.Assembly
    val prefix                                           = Prefix("esw")
    val leaseDuration                                    = Duration.ofSeconds(5)

    "send lock message to component" in {
      val componentRef = TestProbe[ComponentMessage]()

      when(locationServiceUtil.resolveComponentRef(componentName, componentType)).thenReturn(Future.successful(componentRef.ref))

      val lockUnlockUtil = new LockUnlockUtil(locationServiceUtil)(actorSystem)
      lockUnlockUtil.jLock(componentName, componentType, prefix, leaseDuration)

      val msg: Lock = componentRef.expectMessageType[Lock]
      msg.source shouldEqual prefix
      msg.leaseDuration shouldEqual FiniteDuration(leaseDuration.toNanos, TimeUnit.NANOSECONDS)
      msg.replyTo.isInstanceOf[ActorRef[LockingResponse]]
    }

    "throw RuntimeException exception when resolve component fails" in {
      val exception         = new RuntimeException("RuntimeException error")
      val expectedException = new ExecutionException(exception)
      val lockUnlockUtil    = new LockUnlockUtil(locationServiceUtil)(actorSystem)

      when(locationServiceUtil.resolveComponentRef(componentName, componentType))
        .thenReturn(Future.failed(exception))

      val lockingResponse: CompletionStage[LockingResponse] =
        lockUnlockUtil.jLock(componentName, componentType, prefix, leaseDuration)

      lockingResponse.toCompletableFuture.isCompletedExceptionally shouldEqual true
      val actualException = intercept[ExecutionException] {
        lockingResponse.toCompletableFuture.get()
      }
      actualException.getMessage shouldEqual expectedException.getMessage
    }

    "throw IllegalArgumentException exception when resolve component fails" in {
      val exception         = new IllegalArgumentException("IllegalArgumentException error")
      val expectedException = new ExecutionException(exception)
      val lockUnlockUtil    = new LockUnlockUtil(locationServiceUtil)(actorSystem)

      when(locationServiceUtil.resolveComponentRef(componentName, componentType)).thenReturn(Future.failed(exception))

      val lockingResponse: CompletionStage[LockingResponse] =
        lockUnlockUtil.jLock(componentName, componentType, prefix, leaseDuration)

      lockingResponse.toCompletableFuture.isCompletedExceptionally shouldEqual true
      val actualException = intercept[ExecutionException] {
        lockingResponse.toCompletableFuture.get()
      }
      actualException.getMessage shouldEqual expectedException.getMessage
    }
  }

  "jUnlock" must {
    implicit val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test")
    val locationServiceUtil: LocationServiceUtil         = mock[LocationServiceUtil]
    val componentName                                    = "test-assembly"
    val componentType                                    = ComponentType.Assembly
    val prefix                                           = Prefix("esw")

    "send unlock message to component" in {
      val componentRef = TestProbe[ComponentMessage]()

      when(locationServiceUtil.resolveComponentRef(componentName, componentType)).thenReturn(Future.successful(componentRef.ref))

      val lockUnlockUtil = new LockUnlockUtil(locationServiceUtil)(actorSystem)
      lockUnlockUtil.jUnlock(componentName, componentType, prefix)

      val msg: Unlock = componentRef.expectMessageType[Unlock]
      msg.source shouldEqual prefix
      msg.replyTo.isInstanceOf[ActorRef[LockingResponse]]
    }

    "throw RuntimeException exception when resolve component fails" in {
      val exception         = new RuntimeException("RuntimeException error")
      val expectedException = new ExecutionException(exception)
      val lockUnlockUtil    = new LockUnlockUtil(locationServiceUtil)(actorSystem)

      when(locationServiceUtil.resolveComponentRef(componentName, componentType))
        .thenReturn(Future.failed(exception))

      val lockingResponse: CompletionStage[LockingResponse] =
        lockUnlockUtil.jUnlock(componentName, componentType, prefix)

      lockingResponse.toCompletableFuture.isCompletedExceptionally shouldEqual true
      val actualException = intercept[ExecutionException] {
        lockingResponse.toCompletableFuture.get()
      }
      actualException.getMessage shouldEqual expectedException.getMessage
    }

    "throw IllegalArgumentException exception when resolve component fails" in {
      val exception         = new IllegalArgumentException("IllegalArgumentException error")
      val expectedException = new ExecutionException(exception)
      val lockUnlockUtil    = new LockUnlockUtil(locationServiceUtil)(actorSystem)

      when(locationServiceUtil.resolveComponentRef(componentName, componentType)).thenReturn(Future.failed(exception))

      val lockingResponse: CompletionStage[LockingResponse] =
        lockUnlockUtil.jUnlock(componentName, componentType, prefix)

      lockingResponse.toCompletableFuture.isCompletedExceptionally shouldEqual true
      val actualException = intercept[ExecutionException] {
        lockingResponse.toCompletableFuture.get()
      }
      actualException.getMessage shouldEqual expectedException.getMessage
    }
  }
}
