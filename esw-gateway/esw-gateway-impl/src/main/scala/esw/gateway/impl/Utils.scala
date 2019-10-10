package esw.gateway.impl

import scala.concurrent.duration.{DurationLong, FiniteDuration}

object Utils {
  def maxFrequencyToDuration(frequency: Int): FiniteDuration = (1000 / frequency).millis
}
