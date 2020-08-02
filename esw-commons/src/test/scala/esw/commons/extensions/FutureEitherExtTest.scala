package esw.commons.extensions

import java.util.concurrent.CompletionStage

import esw.commons.extensions.FutureEitherExt.{FutureEitherJavaOps, FutureEitherOps}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FutureEitherExtTest extends AnyWordSpec with TypeCheckedTripleEquals with Matchers with ScalaFutures {
  private val rightValue: Either[String, Int]          = Right(42)
  private val leftValue: Either[String, Int]           = Left("Error")
  private def rightFuture: Future[Either[String, Int]] = Future(rightValue)
  private def leftFuture: Future[Either[String, Int]]  = Future(leftValue)

  "mapRight" must {
    "apply function to the right side value of either" in {
      rightFuture.mapRight(_ + 1).futureValue should ===(Right(43))
    }
    "short circuit when right side value not present" in {
      leftFuture.mapRight(_ + 1).futureValue should ===(leftValue)
    }
  }

  "mapRightE" must {
    "apply function to the right side value of either" in {
      rightFuture.mapRightE(i => Right(i + 1)).futureValue should ===(Right(43))
    }
    "apply function to the right side value of either and convert it to left" in {
      rightFuture.mapRightE(_ => Left("Error")).futureValue should ===(Left("Error"))
    }
    "short circuit when right side value not present" in {
      leftFuture.mapRightE(_ => Left("Error")).futureValue should ===(leftValue)
    }
  }

  "mapLeft" must {
    "apply function to the left side value of either" in {
      leftFuture.mapLeft(_ + "1").futureValue should ===(Left("Error1"))
    }
    "short circuit when left side value not present" in {
      rightFuture.mapLeft(_ + "1").futureValue should ===(rightValue)
    }
  }

  "mapError" must {
    "map exceptions to the left type of either" in {
      def exceptionalEither: Future[Either[String, Int]] = Future(throw new RuntimeException("Boom!"))
      exceptionalEither.mapError(_.getMessage).futureValue should ===(Left("Boom!"))
    }
  }

  "flatMapRight" must {
    "apply function to the right side value of either" in {
      rightFuture.flatMapRight(i => Future(i + 1)).futureValue should ===(Right(43))
    }
    "short circuit when right side value not present" in {
      leftFuture.flatMapRight(i => Future(i + 1)).futureValue should ===(leftValue)
    }
  }

  "flatMapRightE" must {
    "apply function to the right side value of either" in {
      rightFuture.flatMapE(i => Future(Right(i + 1))).futureValue should ===(Right(43))
    }
    "apply function to the right side value of either and convert it to left" in {
      rightFuture.flatMapE(_ => Future(Left("Error"))).futureValue should ===(Left("Error"))
    }
    "short circuit when right side value not present" in {
      leftFuture.flatMapE(i => Future(Right(i + 1))).futureValue should ===(leftValue)
    }
  }
  sealed trait Response
  case object Success extends Response
  case object Failure extends Response

  "mapToAdt" must {
    "convert Either type to flat ADT" in {
      val adt = rightFuture.mapToAdt(_ => Success, _ => Failure).futureValue
      adt shouldBe a[Response]
      adt should ===(Success)
    }
    "short circuit when right side value not present" in {
      val adt = leftFuture.mapToAdt(_ => Success, _ => Failure).futureValue
      adt shouldBe a[Response]
      adt should ===(Failure)
    }
  }

  "flatMapToAdt" must {
    "convert Either type to flat ADT" in {
      val adt = rightFuture.flatMapToAdt(_ => Future(Success), _ => Future(Failure)).futureValue
      adt shouldBe a[Response]
      adt should ===(Success)
    }
    "short circuit when right side value not present" in {
      val adt = leftFuture.flatMapToAdt(_ => Future(Success), _ => Failure).futureValue
      adt shouldBe a[Response]
      adt should ===(Failure)
    }
  }

  "toJava" must {
    sealed trait Failure     extends Exception
    case object DivideByZero extends Exception

    def rightFuture: Future[Either[Failure, Int]] = Future(Right(42))
    def leftFuture: Future[Either[Failure, Int]]  = Future(throw DivideByZero)

    "convert scala Future[Either] to Java's CompletionStage[T]" in {
      val toJava = rightFuture.toJava
      toJava shouldBe a[CompletionStage[_]]
      toJava.toCompletableFuture.get() should ===(42)
    }

    "throw an exception when left value present" in {
      val toJava = leftFuture.toJava
      toJava shouldBe a[CompletionStage[_]]
      val exception = intercept[Exception] { toJava.toCompletableFuture.get() }
      exception.getCause shouldBe a[DivideByZero.type]
    }
  }
}
