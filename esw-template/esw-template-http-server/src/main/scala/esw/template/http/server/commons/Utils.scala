package esw.template.http.server.commons

import akka.http.scaladsl.server.Directives.reject
import akka.http.scaladsl.server.{Directive, Directive0, ValidationRejection}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Utils {
  private[esw] def validateFrequency(maxFrequency: Option[Int]): Directive0 = maxFrequency match {
    case Some(0) => reject(ValidationRejection("Max frequency should be greater than zero"))
    case _       => Directive.Empty
  }

  private[esw] def maxFrequencyToDuration(frequency: Int): FiniteDuration = (1000 / frequency).millis

}
