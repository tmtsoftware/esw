package esw.shell.utils

import akka.util.Timeout

import scala.concurrent.duration._

object Timeouts {
  implicit val defaultTimeout: Timeout = 10.seconds
}
