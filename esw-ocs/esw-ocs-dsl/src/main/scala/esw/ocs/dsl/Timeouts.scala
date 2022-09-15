/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl

import scala.concurrent.duration.{DurationLong, FiniteDuration}

private[dsl] object Timeouts {
  val DefaultTimeout: FiniteDuration = 10.seconds
}
