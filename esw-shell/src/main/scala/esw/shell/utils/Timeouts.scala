package esw.shell.utils

import org.apache.pekko.util.Timeout

import scala.concurrent.duration.*

object Timeouts {
  implicit val defaultTimeout: Timeout = 10.seconds
}
