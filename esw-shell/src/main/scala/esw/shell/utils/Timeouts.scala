/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.shell.utils

import akka.util.Timeout

import scala.concurrent.duration._

object Timeouts {
  implicit val defaultTimeout: Timeout = 10.seconds
}
