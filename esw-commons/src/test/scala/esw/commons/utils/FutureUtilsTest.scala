package esw.commons.utils

import esw.commons.BaseTestSuite

import scala.concurrent.Future

class FutureUtilsTest extends BaseTestSuite {
  "firstCompletedOf" must {
    import scala.concurrent.ExecutionContext.Implicits.global

    "return first completed Future based on predicate" in {
      val future1 = Future.successful(1)
      val future2 = Future { Thread.sleep(100); 2 }
      val future3 = Future { Thread.sleep(10); 3 }

      val result = FutureUtils.firstCompletedOf(List(future1, future2, future3))(_ > 1).awaitResult

      result shouldBe Some(3)
    }

    "return None if futures don't matches predicate" in {
      val future1 = Future.successful(1)
      val future2 = Future { Thread.sleep(100); 2 }
      val future3 = Future { Thread.sleep(10); 3 }

      val result = FutureUtils.firstCompletedOf(List(future1, future2, future3))(_ > 3).awaitResult

      result shouldBe None
    }

    "return failed future if none of futures matches predicate and future is failed" in {
      val future1 = Future.successful(1)
      val future2 = Future { Thread.sleep(30); 2 }
      val future3 = Future { Thread.sleep(10); 3 }
      val future4 = Future { Thread.sleep(20); throw new RuntimeException("failed future") }

      val exception =
        intercept[RuntimeException](FutureUtils.firstCompletedOf(List(future1, future2, future3, future4))(_ > 3).awaitResult)

      exception.getMessage shouldBe "failed future"
    }
  }
}
