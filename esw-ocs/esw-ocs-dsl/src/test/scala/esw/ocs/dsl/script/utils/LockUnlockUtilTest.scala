package esw.ocs.dsl.script.utils

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.command.client.messages.ComponentMessage
import csw.command.client.messages.SupervisorLockMessage.{Lock, Unlock}
import csw.command.client.models.framework.LockingResponse
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.testcommons.BaseTestSuite

import java.time.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class LockUnlockUtilTest extends BaseTestSuite {

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")

  override def afterAll(): Unit = actorSystem.terminate()

  "Lock" must {
    val source        = Prefix(ESW, "sequencer")
    val leaseDuration = Duration.ofSeconds(5)

    "send lock message to component | ESW-126" in {
      val componentRef = TestProbe[ComponentMessage]()

      val lockUnlockUtil = new LockUnlockUtil(source)(actorSystem)
      lockUnlockUtil.lock(componentRef.ref, leaseDuration)(() => ???, () => ???)

      val msg: Lock = componentRef.expectMessageType[Lock]
      msg.source shouldEqual source
      msg.leaseDuration shouldEqual FiniteDuration(leaseDuration.toNanos, TimeUnit.NANOSECONDS)
      msg.replyTo.isInstanceOf[ActorRef[LockingResponse]]
    }

//     "throw RuntimeException exception when resolve component fails | ESW-126" in {
//      val exception      = new RuntimeException("RuntimeException error")
//      val lockUnlockUtil = new LockUnlockUtil(source)(actorSystem)
//      val componentRef   = TestProbe[ComponentMessage]()
//
//      val lockingResponse: CompletionStage[LockingResponse] =
//        lockUnlockUtil.lock(componentRef.ref, leaseDuration)(() => ???, () => ???)
//
//      val actualException = intercept[RuntimeException] {
//        lockingResponse.toCompletableFuture.asScala.awaitResult
//      }
//      actualException.getMessage shouldEqual exception.getMessage
//    }
  }

  "Unlock" must {
    val componentName = "test_assembly"
    val source        = Prefix(ESW, componentName)

    "send unlock message to component | ESW-126" in {
      val componentRef = TestProbe[ComponentMessage]()

      val lockUnlockUtil = new LockUnlockUtil(source)(actorSystem)
      lockUnlockUtil.unlock(componentRef.ref)

      val msg: Unlock = componentRef.expectMessageType[Unlock]
      msg.source shouldEqual source
      msg.replyTo.isInstanceOf[ActorRef[LockingResponse]]
    }

//    "throw RuntimeException exception when resolve component fails | ESW-126" in {
//      val componentRef      = TestProbe[ComponentMessage]()
//      val source            = Prefix(ESW, "component")
//      val exception         = new RuntimeException("RuntimeException error")
//      val expectedException = new ExecutionException(exception)
//      val lockUnlockUtil    = new LockUnlockUtil(source)(actorSystem)
//
//      val lockingResponse: CompletionStage[LockingResponse] =
//        lockUnlockUtil.unlock(componentRef.ref)
//
//      lockingResponse.toCompletableFuture.isCompletedExceptionally shouldEqual true
//      val actualException = intercept[ExecutionException] {
//        lockingResponse.toCompletableFuture.get()
//      }
//      actualException.getMessage shouldEqual expectedException.getMessage
//    }
  }
}
