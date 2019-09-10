package esw.ocs.impl.internal

import scala.concurrent.duration.{DurationLong, FiniteDuration}

private[ocs] object Timeouts {
  val DefaultTimeout: FiniteDuration = 30.seconds
  val LongTimeout: FiniteDuration    = 10.hours
}
