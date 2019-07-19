package esw.ocs.internal

import scala.concurrent.duration.{DurationLong, FiniteDuration}

private[ocs] object Timeouts {
  val DefaultTimeout: FiniteDuration = 30.seconds
  val EngineTimeout: FiniteDuration  = 10.hour
}
