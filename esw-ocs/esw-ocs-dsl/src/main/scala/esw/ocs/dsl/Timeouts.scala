package esw.ocs.dsl

import scala.concurrent.duration.{DurationLong, FiniteDuration}

private[dsl] object Timeouts {
  val DefaultTimeout: FiniteDuration = 10.seconds
}
