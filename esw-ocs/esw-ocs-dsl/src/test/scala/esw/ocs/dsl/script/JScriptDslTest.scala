package esw.ocs.dsl.script

import java.util.concurrent.CompletableFuture

import csw.params.commands.{CommandName, Observe, Setup}
import csw.params.core.models.Prefix
import esw.ocs.api.BaseTestSuite

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.DurationDouble

class JScriptDslTest extends BaseTestSuite {
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(20.seconds)

  "ScriptDsl" must {
    "allow adding and executing setup handler" in {
      var receivedPrefix: Option[Prefix] = None

      val csw: CswServices = mock[CswServices]
      val script: MainScriptDsl = new MainScriptDsl(csw) {
        override protected implicit def strandEc: StrandEc = StrandEc()

        onSetupCommand("iris") { cmd =>
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
      val script: MainScriptDsl = new MainScriptDsl(csw) {
        override protected implicit def strandEc: StrandEc = StrandEc()

        onObserveCommand("iris") { cmd =>
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
      val script: MainScriptDsl = new MainScriptDsl(csw) {
        override protected implicit def strandEc: StrandEc = StrandEc()

        onShutdown {
          orderOfShutdownCalled += 1
          () => CompletableFuture.completedFuture(null)
        }

        onShutdown {
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
      val script: MainScriptDsl = new MainScriptDsl(csw) {
        override protected implicit def strandEc: StrandEc = StrandEc()

        onAbortSequence {
          orderOfAbortCalled += 1
          () => CompletableFuture.completedFuture(null)
        }

        onAbortSequence {
          orderOfAbortCalled += 2
          () => CompletableFuture.completedFuture(null)
        }
      }

      script.executeAbort().futureValue
      orderOfAbortCalled shouldBe ArrayBuffer(1, 2)
    }

    "allow adding and executing exception handlers | ESW-139" in {
      var receivedPrefix: Option[Throwable] = None

      val csw: CswServices = mock[CswServices]
      val script: MainScriptDsl = new MainScriptDsl(csw) {
        override protected implicit def strandEc: StrandEc = StrandEc()

        onException { ex =>
          receivedPrefix = Some(ex)
          CompletableFuture.completedFuture(null)
        }
      }

      val exception = new RuntimeException("testing exception handlers")
      script.executeExceptionHandlers(exception).toCompletableFuture.get()

      receivedPrefix.value shouldBe exception
    }
  }
}
