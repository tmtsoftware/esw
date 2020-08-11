package esw.agent.akka.app.ext

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import esw.agent.akka.app.ext.FutureExt.FutureOps
import esw.testcommons.BaseTestSuite

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future, TimeoutException}

class FutureExtTest extends BaseTestSuite {
  implicit private lazy val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "future-ext")
  import system.executionContext

  override protected def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.futureValue
  }

  "timeout" must {
    "throw TimeoutException when future does not complete within provided duration" in {
      val future = Future { Thread.sleep(1000) }
      a[TimeoutException] shouldBe thrownBy(Await.result(future.timeout(10.millis), 5.seconds))
    }

    "not throw exception and future should finish within the timeout" in {
      val future = Future { Thread.sleep(10); 42 }
      val result = future.timeout(1.seconds).futureValue
      result should ===(42)
    }
  }
}
