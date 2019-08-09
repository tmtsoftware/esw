package esw.ocs.dsl

import java.util.concurrent.atomic.AtomicInteger

import akka.Done
import esw.ocs.api.BaseTestSuite
import org.scalatest.time.SpanSugar.convertFloatToGrainOfTime

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class ControlDslTest extends BaseTestSuite {

  class TestDsl() extends ControlDsl {
    val loopInterval: FiniteDuration = 500.millis
    def counterLoop(minimumDelay: Option[FiniteDuration] = None): (() => Int, Future[Done]) = {
      var counter = 0

      def increment: Future[StopIf] = Future {
        counter += 1
        stopIf(counter == 3)
      }

      val loopFinished =
        minimumDelay match {
          case Some(delay) => loop(delay)(increment)
          case None        => loop(increment)
        }

      (() => counter, loopFinished)
    }
  }

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(1.second)

  "loop" must {
    "run till condition becomes true when interval is default | ESW-89" in {
      val testDsl                    = new TestDsl
      val (getCounter, loopFinished) = testDsl.counterLoop()

      // default interval is 50ms, loop will fin
      loopFinished.isReadyWithin(500.millis) shouldBe false
      getCounter() should be < 3
      loopFinished.futureValue shouldBe Done
      getCounter() shouldBe 3
    }

    "run till condition becomes true when interval is custom | ESW-89" in {
      val testDsl                    = new TestDsl
      val (getCounter, loopFinished) = testDsl.counterLoop(Some(400.millis))

      loopFinished.isReadyWithin(300.millis) shouldBe false
      getCounter() shouldBe 1

      loopFinished.isReadyWithin(400.millis) shouldBe false
      getCounter() shouldBe 2

      loopFinished.futureValue shouldBe Done
      getCounter() shouldBe 3
    }
  }

  "par" must {
    "execute provided list of tasks asynchronously | ESW-87" in {

      import Implicits.global

      class TestDsl() extends ControlDsl {
        val loopInterval: FiniteDuration = 500.millis

        def execute[T](tasks: Future[T]*): Future[List[T]] = par(tasks: _*)
      }

      val counter = new AtomicInteger(0)
      val testDsl = new TestDsl

      val result = testDsl.execute(
        Future(counter.getAndIncrement()),
        Future(counter.getAndIncrement()),
        Future(counter.getAndIncrement())
      )

      result.futureValue.toSet should ===(Set(0, 1, 2))
    }
  }
}
