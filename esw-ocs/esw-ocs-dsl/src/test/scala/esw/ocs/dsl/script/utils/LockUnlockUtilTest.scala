package esw.ocs.dsl.script.utils

import java.time.Duration
import java.util.concurrent.TimeUnit

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.command.client.messages.ComponentMessage
import csw.command.client.messages.SupervisorLockMessage.{Lock, Unlock}
import csw.command.client.models.framework.LockingResponse
import csw.location.models.ComponentType
import csw.params.core.models.Prefix
import csw.params.core.models.Subsystem.{ESW, TCS}
import esw.ocs.api.BaseTestSuite
import esw.ocs.dsl.sequence_manager.LocationServiceUtil

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class LockUnlockUtilTest extends BaseTestSuite {

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")

  override def afterAll() = actorSystem.terminate()

  "Lock" must {
    val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]
    val componentName                            = "test_assembly"
    val source                                   = Prefix(ESW, "sequencer")
    val destination                              = Prefix(TCS, componentName)
    val leaseDuration                            = Duration.ofSeconds(5)
    val componentType                            = ComponentType.Assembly

    "send lock message to component | ESW-126" in {
      val componentRef = TestProbe[ComponentMessage]()

      when(locationServiceUtil.resolveComponentRef(destination, componentType))
        .thenReturn(Future.successful(componentRef.ref))

      val lockUnlockUtil = new LockUnlockUtil(locationServiceUtil)(actorSystem)
      lockUnlockUtil.lock(componentRef.ref, source, leaseDuration)(() => ???, () => ???)

      val msg: Lock = componentRef.expectMessageType[Lock]
      msg.source shouldEqual source
      msg.leaseDuration shouldEqual FiniteDuration(leaseDuration.toNanos, TimeUnit.NANOSECONDS)
      msg.replyTo.isInstanceOf[ActorRef[LockingResponse]]
    }

//    "throw RuntimeException exception when resolve component fails | ESW-126" in {
//      val destination    = Prefix(TCS, componentName)
//      val exception      = new RuntimeException("RuntimeException error")
//      val lockUnlockUtil = new LockUnlockUtil(locationServiceUtil)(actorSystem)
//      val componentRef   = TestProbe[ComponentMessage]()
//
//      when(locationServiceUtil.resolveComponentRef(destination, componentType))
//        .thenReturn(Future.failed(exception))
//
//      val lockingResponse: CompletionStage[LockingResponse] =
//        lockUnlockUtil.lock(componentRef.ref, source, leaseDuration)(() => ???, () => ???)
//
//      val actualException = intercept[RuntimeException] {
//        lockingResponse.asScala.awaitResult
//      }
//      actualException.getMessage shouldEqual exception.getMessage
//    }
  }

  "Unlock" must {
    val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]
    val componentName                            = "test_assembly"
    val destination                              = Prefix(TCS, componentName)
    val componentType                            = ComponentType.Assembly
    val source                                   = Prefix(ESW, componentName)

    "send unlock message to component | ESW-126" in {
      val componentRef = TestProbe[ComponentMessage]()

      when(locationServiceUtil.resolveComponentRef(destination, componentType)).thenReturn(Future.successful(componentRef.ref))

      val lockUnlockUtil = new LockUnlockUtil(locationServiceUtil)(actorSystem)
      lockUnlockUtil.unlock(componentRef.ref, source)

      val msg: Unlock = componentRef.expectMessageType[Unlock]
      msg.source shouldEqual source
      msg.replyTo.isInstanceOf[ActorRef[LockingResponse]]
    }

//    "throw RuntimeException exception when resolve component fails | ESW-126" in {
//      val componentRef      = TestProbe[ComponentMessage]()
//      val source            = Prefix(ESW, "component")
//      val destination       = Prefix(TCS, componentName)
//      val exception         = new RuntimeException("RuntimeException error")
//      val expectedException = new ExecutionException(exception)
//      val lockUnlockUtil    = new LockUnlockUtil(locationServiceUtil)(actorSystem)
//
//      when(locationServiceUtil.resolveComponentRef(destination, componentType))
//        .thenReturn(Future.failed(exception))
//
//      val lockingResponse: CompletionStage[LockingResponse] =
//        lockUnlockUtil.unlock(componentRef.ref, source)
//
//      lockingResponse.toCompletableFuture.isCompletedExceptionally shouldEqual true
//      val actualException = intercept[ExecutionException] {
//        lockingResponse.toCompletableFuture.get()
//      }
//      actualException.getMessage shouldEqual expectedException.getMessage
//    }
  }
}
