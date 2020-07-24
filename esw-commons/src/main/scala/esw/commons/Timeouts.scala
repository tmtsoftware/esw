package esw.commons

import scala.concurrent.duration.{DurationLong, FiniteDuration}

object Timeouts {
  val DefaultTimeout: FiniteDuration = 30.seconds

  // This timeout is extracted here so application can use consistent default timeout for resolve
  // fixme: Try to drive timeout from layers composing on top of locationService.resolve instead of this default value
  val DefaultResolveLocationDuration: FiniteDuration = 3.seconds
  val LongTimeout: FiniteDuration                    = 10.hours
}
