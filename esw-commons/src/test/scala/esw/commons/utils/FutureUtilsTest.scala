package esw.commons.utils

import java.util.concurrent.{Executors, ScheduledExecutorService}

import esw.commons.BaseTestSuite

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

class FutureUtilsTest extends BaseTestSuite {

  implicit val executorService: ScheduledExecutorService = Executors.newScheduledThreadPool(4)
  implicit val ec: ExecutionContext                      = ExecutionContext.fromExecutorService(executorService)

  override def afterAll(): Unit = {
    super.afterAll()
    executorService.shutdown()
  }

  "firstCompletedOf" must {
    "return first completed Future based on predicate" in {
      val future1 = TestSetup.future(delay = 1.millis, value = 1)
      val future2 = TestSetup.future(delay = 100.millis, value = 2)
      val future3 = TestSetup.future(delay = 10.millis, value = 3)
      val future4 = TestSetup.future(delay = 10.millis, value = throw new RuntimeException("failed"))

      val result = FutureUtils.firstCompletedOf(List(future1, future2, future3, future4))(_ > 1).awaitResult

      result shouldBe Some(3)
    }

    "return None if futures don't matches predicate" in {
      val future1 = TestSetup.future(delay = 1.millis, value = 1)
      val future2 = TestSetup.future(delay = 100.millis, value = 2)
      val future3 = TestSetup.future(delay = 10.millis, value = 3)

      val result = FutureUtils.firstCompletedOf(List(future1, future2, future3))(_ > 3).awaitResult

      result shouldBe None
    }

    "return failed future if none of futures matches predicate and future is failed" in {
      val future1 = TestSetup.future(delay = 1.millis, value = 1)
      val future2 = TestSetup.future(delay = 30.millis, value = 2)
      val future3 = TestSetup.future(delay = 10.millis, value = throw new RuntimeException("failed"))

      val exception =
        intercept[RuntimeException](FutureUtils.firstCompletedOf(List(future1, future2, future3))(_ > 3).awaitResult)

      exception.getMessage shouldBe "failed"
    }
  }

  object TestSetup {
    def future[T](delay: FiniteDuration, value: => T)(
        implicit executorService: ScheduledExecutorService
    ): Future[T] = {
      val p = Promise[T]()
      executorService.schedule(() => p.tryComplete(Try(value)), delay.length, delay.unit)
      p.future
    }
  }
}
