package esw.gateway.impl

import scala.concurrent.duration.*

object Utils {
  def maxFrequencyToDuration(frequency: Int): FiniteDuration = (1000 / frequency).millis
}
