package esw.ocs.framework.internal

import scala.concurrent.duration.{DurationLong, FiniteDuration}

private[framework] object Timeouts {
  val DefaultTimeout: FiniteDuration = 30.seconds
  val EngineTimeout: FiniteDuration  = 10.hour
}
