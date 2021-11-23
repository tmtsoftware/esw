package esw.ocs.dsl.script

import csw.logging.api.javadsl.ILogger
import csw.params.commands.{CommandName, Observe, Setup}
import csw.prefix.models.Prefix
import esw.ocs.impl.core.SequenceOperator
import esw.testcommons.BaseTestSuite

import java.util.concurrent.CompletableFuture
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.DurationDouble

class ScriptDslTest extends BaseTestSuite {
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(20.seconds)

  private val strandEc               = StrandEc()
  private val shutdownTask: Runnable = () => ()
  private val logger                 = mock[ILogger]

  override protected def afterAll(): Unit = strandEc.shutdown()

  "ScriptDsl" must {
    "allow adding and executing setup handler" in {
      var receivedPrefix: Option[Prefix] = None

      // todo : extract a beforeEach
      val seqOperatorFactory = () => mock[SequenceOperator]
      val script: ScriptDsl = new ScriptDsl(seqOperatorFactory, logger, strandEc, shutdownTask) {

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

      val seqOperatorFactory = () => mock[SequenceOperator]
      val script: ScriptDsl = new ScriptDsl(seqOperatorFactory, logger, strandEc, shutdownTask) {

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

      val seqOperatorFactory = () => mock[SequenceOperator]
      val script: ScriptDsl = new ScriptDsl(seqOperatorFactory, logger, strandEc, shutdownTask) {

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

      val seqOperatorFactory = () => mock[SequenceOperator]
      val script: ScriptDsl = new ScriptDsl(seqOperatorFactory, logger, strandEc, shutdownTask) {

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

      val seqOperatorFactory = () => mock[SequenceOperator]
      val script: ScriptDsl = new ScriptDsl(seqOperatorFactory, logger, strandEc, shutdownTask) {

        onException { ex =>
          receivedPrefix = Some(ex)
          CompletableFuture.completedFuture(null)
        }
      }

      val exception = new RuntimeException("testing exception handlers")
      script.executeExceptionHandlers(exception).futureValue

      receivedPrefix.value shouldBe exception
    }

    "shutdownScript" must {
      "execute the shutdown task" in {
        val seqOperatorFactory = () => mock[SequenceOperator]
        var taskCalled         = false
        val shutdownTask: Runnable = () => {
          taskCalled = true
        }
        val script: ScriptDsl = new ScriptDsl(seqOperatorFactory, logger, strandEc, shutdownTask)
        script.shutdownScript()

        taskCalled shouldBe true
      }
    }
  }
}
