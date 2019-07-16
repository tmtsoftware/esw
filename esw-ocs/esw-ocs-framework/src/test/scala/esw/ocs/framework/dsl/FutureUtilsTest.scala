package esw.ocs.framework.dsl

import esw.ocs.async.macros.StrandEc
import esw.ocs.framework.BaseTestSuite
import esw.ocs.framework.dsl.internal.FutureUtils

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class FutureUtilsTest extends BaseTestSuite {
  implicit var strandEc: StrandEc = _

  override protected def beforeEach(): Unit = strandEc = StrandEc()
  override protected def afterEach(): Unit  = strandEc.shutdown()

  "delayedResult" must {
    "complete the future after minDelay when min delay > function completion duration | ESW-90" in {
      var counter = 0

      val task: Future[Boolean] = FutureUtils.delayedResult(500.millis) {
        counter += 1
        Future.successful(true)
      }

      //after 200ms ensure that future not completed but function body is executed
      task.isReadyWithin(200.millis) shouldBe false
      counter shouldBe 1

      //after more 200ms, ensure that future is still not complete
      task.isReadyWithin(200.millis) shouldBe false

      //eventually, ensure that future is complete (after 500ms)
      task.futureValue shouldBe true
    }

    "complete the future after function completion when min delay < function completion duration | ESW-90" in {
      val task: Future[Boolean] = FutureUtils.delayedResult(1.millis) {
        Thread.sleep(500)
        Future.successful(true)
      }

      //after 400ms ensure that future not completed as function takes more than 500ms
      task.isReadyWithin(400.millis) shouldBe false

      //eventually, ensure that future is complete (after 500ms)
      task.futureValue shouldBe true
    }
  }
}
