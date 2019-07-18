package esw.ocs.framework.dsl

import java.util.concurrent.CountDownLatch

import csw.params.commands.{CommandName, Observe, Setup}
import csw.params.core.models.Prefix
import esw.ocs.framework.BaseTestSuite

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration.{DurationDouble, FiniteDuration}

class ScriptDslTest extends BaseTestSuite {

  "ScriptDsl" must {
    "allow adding and executing setup handler" in {
      var receivedPrefix: Option[Prefix] = None

      val script: ScriptDsl = new ScriptDsl {
        override def csw: CswServices             = ???
        override val loopInterval: FiniteDuration = 100.millis

        handleSetupCommand("iris") { cmd =>
          spawn {
            receivedPrefix = Some(cmd.source)
            ()
          }
        }
      }
      val prefix    = Prefix("iris.move")
      val irisSetup = Setup(prefix, CommandName("iris"), None)
      script.execute(irisSetup).futureValue

      receivedPrefix.value shouldBe prefix
    }

    "allow adding and executing observe handler" in {
      var receivedPrefix: Option[Prefix] = None

      val script: ScriptDsl = new ScriptDsl {
        override def csw: CswServices             = ???
        override val loopInterval: FiniteDuration = 100.millis
        handleObserveCommand("iris") { cmd =>
          spawn {
            receivedPrefix = Some(cmd.source)
            ()
          }
        }
      }
      val prefix      = Prefix("iris.move")
      val irisObserve = Observe(prefix, CommandName("iris"), None)
      script.execute(irisObserve).futureValue

      receivedPrefix.value shouldBe prefix
    }

    "allow adding and executing multiple shutdown handlers in order" in {
      val orderOfShutdownCalled = ArrayBuffer.empty[Int]

      val script: ScriptDsl = new ScriptDsl {
        override def csw: CswServices             = ???
        override val loopInterval: FiniteDuration = 100.millis
        handleShutdown {
          spawn {
            orderOfShutdownCalled += 1
          }
        }

        handleShutdown {
          spawn {
            orderOfShutdownCalled += 2
          }
        }
      }

      script.executeShutdown().futureValue
      orderOfShutdownCalled shouldBe ArrayBuffer(1, 2)
    }

    "allow adding and executing multiple abort handlers in order" in {
      val orderOfAbortCalled = ArrayBuffer.empty[Int]

      val script: ScriptDsl = new ScriptDsl {
        override def csw: CswServices             = ???
        override val loopInterval: FiniteDuration = 100.millis
        handleAbort {
          spawn {
            orderOfAbortCalled += 1
          }
        }

        handleAbort {
          spawn {
            orderOfAbortCalled += 2
          }
        }
      }

      script.executeAbort().futureValue
      orderOfAbortCalled shouldBe ArrayBuffer(1, 2)
    }

    "allow running operations sequentially | ESW-88" in {

      val latch = new CountDownLatch(3)
      val script: ScriptDsl = new ScriptDsl {
        override def csw: CswServices             = ???
        override val loopInterval: FiniteDuration = 100.millis

        def decrement: Future[Unit] = Future { latch.countDown() }

        handleSetupCommand("iris") { _ =>
          spawn {

            // await utility provided in ControlDsl, asynchronously blocks for future to complete
            decrement.await
            decrement.await
            decrement.await
          }
        }
      }

      val prefix    = Prefix("iris.move")
      val irisSetup = Setup(prefix, CommandName("iris"), None)
      script.execute(irisSetup).futureValue

      latch.getCount should ===(0L)
    }
  }

}
