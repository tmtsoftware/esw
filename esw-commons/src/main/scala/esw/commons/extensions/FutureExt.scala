/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.commons.extensions

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

/**
 * This is an extension class containing convenience functions for handling use cases involving Future.
 */
object FutureExt {
  implicit class FutureOps[T](val future: Future[T]) extends AnyVal {
    def await(duration: FiniteDuration = 10.seconds): T = Await.result(future, duration)
    def get: T                                          = future.await()
  }
}
