package esw.testcommons

import akka.actor.typed.ActorSystem
import csw.prefix.models.Subsystem
import org.mockito.MockitoSugar
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.{Duration, DurationDouble, FiniteDuration}
import scala.concurrent.{Future, Promise}
import scala.util.{Random, Try}

trait BaseTestSuite
    extends AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with OptionValues
    with EitherValues
    with MockitoSugar
    with TypeCheckedTripleEquals
    with Eventually {
  val defaultTimeout: Duration = 10.seconds

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  implicit class EitherOps[L, R](either: Either[L, R]) {
    def rightValue: R = either.toOption.get
    def leftValue: L  = either.left.value
  }

  implicit class FutureEitherOps[L, R](futureEither: Future[Either[L, R]]) {
    def rightValue: R = futureEither.futureValue.rightValue
    def leftValue: L  = futureEither.futureValue.leftValue
  }

  def future[T](delay: FiniteDuration, value: => T)(implicit system: ActorSystem[_]): Future[T] = {
    import system.executionContext
    val scheduler = system.scheduler
    val p         = Promise[T]()
    scheduler.scheduleOnce(delay, () => p.tryComplete(Try(value)))
    p.future
  }

  def futureRight[T](value: T): Future[Right[Nothing, T]] = Future.successful(Right(value))
  def futureLeft[T](value: T): Future[Left[T, Nothing]]   = Future.successful(Left(value))

  def randomFrom[T](values: List[T]): T = values(Random.nextInt(values.size))
  def randomSubsystem: Subsystem        = randomFrom(Subsystem.values.toList)
  def randomString(size: Int): String   = Random.alphanumeric.take(size).mkString
  def randomInt(till: Int): Int         = Random.nextInt(till)
}
