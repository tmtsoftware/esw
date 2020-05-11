package esw.commons.utils

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import esw.commons.BaseTestSuite

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationLong

class FutureUtilsTest extends BaseTestSuite {

  implicit val system: ActorSystem[_] =
    ActorSystem(Behaviors.empty, "future-test")

  implicit val ec: ExecutionContext = system.executionContext

  override def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.futureValue
  }

  "firstCompletedOf" must {
    "return first completed Future based on predicate" in {
      val future1 = future(delay = 1.millis, value = 1)
      val future2 = future(delay = 100.millis, value = 2)
      val future3 = future(delay = 50.millis, value = 3)
      val future4 =
        future(delay = 1.millis, value = throw new RuntimeException("failed"))

      val result = FutureUtils
        .firstCompletedOf(List(future1, future2, future3, future4))(_ == 3)
        .futureValue

      result shouldBe Some(3)
    }

    "return None if list of futures is empty" in {
      val result = FutureUtils
        .firstCompletedOf(List.empty[Future[Int]])(_ == 3)
        .futureValue

      result shouldBe None
    }

    "return None if futures don't matches predicate" in {
      val future1 = future(delay = 1.millis, value = 1)
      val future2 = future(delay = 100.millis, value = 2)
      val future3 = future(delay = 10.millis, value = 3)

      val result = FutureUtils
        .firstCompletedOf(List(future1, future2, future3))(_ > 3)
        .futureValue

      result shouldBe None
    }

    "return none when predicate did not match and some of the futures failed" in {
      val future1 = future(delay = 1.millis, value = 1)
      val future2 = future(delay = 30.millis, value = 2)
      val future3 =
        future(delay = 10.millis, value = throw new RuntimeException("failed"))

      val result = FutureUtils
        .firstCompletedOf(List(future1, future2, future3))(_ > 3)
        .futureValue
      result shouldBe None
    }
  }
}
