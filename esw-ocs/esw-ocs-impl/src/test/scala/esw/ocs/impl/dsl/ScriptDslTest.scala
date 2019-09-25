package esw.ocs.impl.dsl

import java.util.concurrent.{CompletableFuture, CountDownLatch}

import csw.params.commands.{CommandName, Observe, Setup}
import csw.params.core.models.Prefix
import esw.dsl.script.CswServices
import esw.dsl.script.javadsl.JScript
import esw.ocs.api.BaseTestSuite
import esw.ocs.macros.StrandEc

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.DurationDouble

class ScriptDslTest extends BaseTestSuite {
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(20.seconds)
  "ScriptDsl" must {
    "allow adding and executing setup handler" in {
      var receivedPrefix: Option[Prefix] = None

      val csw: CswServices = mock[CswServices]
      val script: JScript = new JScript(csw) {
        override protected implicit def strandEc: StrandEc = StrandEc()

        jHandleSetupCommand("iris") { cmd =>
          receivedPrefix = Some(cmd.source)
          CompletableFuture.completedFuture(null)
        }
      }

      val prefix    = Prefix("iris.move")
      val irisSetup = Setup(prefix, CommandName("iris"), None)
      script.execute(irisSetup).futureValue

      receivedPrefix.value shouldBe prefix
    }

    "allow adding and executing observe handler" in {
      var receivedPrefix: Option[Prefix] = None

      val csw: CswServices = mock[CswServices]
      val script: JScript = new JScript(csw) {
        override protected implicit def strandEc: StrandEc = StrandEc()

        jHandleObserveCommand("iris") { cmd =>
          receivedPrefix = Some(cmd.source)
          CompletableFuture.completedFuture(null)
        }
      }

      val prefix      = Prefix("iris.move")
      val irisObserve = Observe(prefix, CommandName("iris"), None)
      script.execute(irisObserve).futureValue

      receivedPrefix.value shouldBe prefix
    }

    "allow adding and executing multiple shutdown handlers in order" in {
      val orderOfShutdownCalled = ArrayBuffer.empty[Int]

      val csw: CswServices = mock[CswServices]
      val script: JScript = new JScript(csw) {
        override protected implicit def strandEc: StrandEc = StrandEc()
        jHandleShutdown {
          orderOfShutdownCalled += 1
          () => CompletableFuture.completedFuture(null)
        }

        jHandleShutdown {
          orderOfShutdownCalled += 2
          () => CompletableFuture.completedFuture(null)
        }
      }

      script.executeShutdown().futureValue
      orderOfShutdownCalled shouldBe ArrayBuffer(1, 2)
    }

    "allow adding and executing multiple abort handlers in order" in {
      val orderOfAbortCalled = ArrayBuffer.empty[Int]

      val csw: CswServices = mock[CswServices]
      val script: JScript = new JScript(csw) {
        override protected implicit def strandEc: StrandEc = StrandEc()
        jHandleAbort {
          orderOfAbortCalled += 1
          () => CompletableFuture.completedFuture(null)
        }

        jHandleAbort {
          orderOfAbortCalled += 2
          () => CompletableFuture.completedFuture(null)
        }
      }

      script.executeAbort().futureValue
      orderOfAbortCalled shouldBe ArrayBuffer(1, 2)
    }

    "allow running operations sequentially | ESW-88" in {

      val latch            = new CountDownLatch(3)
      val csw: CswServices = mock[CswServices]
      val script: JScript = new JScript(csw) {
        override protected implicit def strandEc: StrandEc = StrandEc()

        def decrement: CompletableFuture[Unit] =
          CompletableFuture.completedFuture { Thread.sleep(100); latch.countDown() }

        jHandleSetupCommand("iris") { _ =>
          // await utility provided in ControlDsl, asynchronously blocks for future to complete
          decrement.get()
          decrement.get()
          decrement.get()
          CompletableFuture.completedFuture(null)
        }
      }

      val prefix    = Prefix("iris.move")
      val irisSetup = Setup(prefix, CommandName("iris"), None)
      script.execute(irisSetup).futureValue

      latch.getCount should ===(0L)
    }
  }

}
