package esw.http.core.commons

import org.apache.pekko.actor.CoordinatedShutdown

object CoordinatedShutdownReasons {
  case object ApplicationFinishedReason   extends CoordinatedShutdown.Reason
  case class FailureReason(ex: Throwable) extends CoordinatedShutdown.Reason
}
