/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.commons.extensions

/**
 * This is an extension class containing convenience functions for handling use cases involving List & Either.
 */
object ListEitherExt {
  implicit class ListEitherOps[L, R](private val eithers: List[Either[L, R]]) extends AnyVal {

    def sequence: Either[List[L], List[R]] =
      eithers.partition(_.isLeft) match {
        case (Nil, success) => Right(for (Right(i) <- success) yield i)
        case (errs, _)      => Left(for (Left(s) <- errs) yield s)
      }

  }
}
