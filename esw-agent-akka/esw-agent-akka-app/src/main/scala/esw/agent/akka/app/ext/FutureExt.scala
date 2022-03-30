/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.agent.akka.app.ext

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.pattern

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, TimeoutException}

/**
 * This is a convenience utility on top of [[scala.concurrent.Future]].
 */
object FutureExt {
  private lazy val timedOutFuture = Future.failed(new TimeoutException)

  implicit class FutureOps[T](private val future: Future[T]) extends AnyVal {
    def timeout(duration: FiniteDuration)(implicit system: ActorSystem[_]): Future[T] = {
      import system.executionContext
      val delayedTimeout = pattern.after(duration, system.toClassic.scheduler)(timedOutFuture)
      Future.firstCompletedOf(List(future, delayedTimeout))
    }
  }

}
