package esw.gateway.impl.utils

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationLong

object Utils {
  def maxFrequencyToDuration(frequency: Int): FiniteDuration = (1000 / frequency).millis
}
