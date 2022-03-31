/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.testkit.utils

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.*
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.{Duration, DurationDouble, FiniteDuration}
import scala.concurrent.{Future, Promise}
import scala.util.Try

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
  val defaultTimeout: Duration          = 10.seconds
  implicit lazy val askTimeout: Timeout = Timeout(10.seconds)

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(defaultTimeout, 50.millis)

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

}
