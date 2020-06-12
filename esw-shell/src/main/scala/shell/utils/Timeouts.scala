package shell.utils

import akka.util.Timeout

import scala.concurrent.duration._

object Timeouts {
  val defaultDuration: FiniteDuration  = 10.seconds
  implicit val defaultTimeout: Timeout = defaultDuration
}
