package esw.ocs.framework.dsl

import akka.Done
import esw.ocs.framework.BaseTestSuite
import org.scalatest.time.SpanSugar.convertFloatToGrainOfTime

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class ControlDslTest extends BaseTestSuite {
  class TestDsl() extends ControlDsl {
    implicit val ec: ExecutionContext                        = toEc
    override def loop(block: ⇒ Future[StopIf]): Future[Done] = super.loop(block)
    override def loop(minimumInterval: FiniteDuration)(block: ⇒ Future[StopIf]): Future[Done] =
      super.loop(minimumInterval)(block)
    override def stopIf(condition: Boolean): StopIf = super.stopIf(condition)
  }

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(1.second)

  "loop" when {
    "interval is default" should {
      "run loop till condition becomes true" in {
        val (getCounter, loopFinished) = counterLoop()

        // default interval is 50ms, loop will fin
        loopFinished.isReadyWithin(50.millis) shouldBe false
        getCounter() should be < 3
        loopFinished.futureValue shouldBe Done
        getCounter() shouldBe 3
      }
    }

    "interval is custom" should {
      "run loop till condition becomes true" in {
        val (getCounter, loopFinished) = counterLoop(Some(100.millis))

        loopFinished.isReadyWithin(50.millis) shouldBe false
        getCounter() shouldBe 1

        loopFinished.isReadyWithin(70.millis) shouldBe false
        getCounter() shouldBe 2

        loopFinished.futureValue shouldBe Done
        getCounter() shouldBe 3
      }
    }
  }

  private def counterLoop(minimumDelay: Option[FiniteDuration] = None) = {
    val testDsl = new TestDsl
    import testDsl._
    var counter = 0

    def increment: Future[StopIf] = Future {
      counter += 1
      stopIf(counter == 3)
    }

    val loopFinished =
      minimumDelay match {
        case Some(delay) ⇒ loop(delay)(increment)
        case None        ⇒ loop(increment)
      }

    (() ⇒ counter, loopFinished)
  }
}
