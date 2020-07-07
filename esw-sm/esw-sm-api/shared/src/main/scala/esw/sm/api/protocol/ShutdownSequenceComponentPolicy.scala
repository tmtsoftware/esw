package esw.sm.api.protocol

import csw.prefix.models.Prefix

sealed trait ShutdownSequenceComponentPolicy

object ShutdownSequenceComponentPolicy {
  case class SingleSequenceComponent(prefix: Prefix) extends ShutdownSequenceComponentPolicy
  case object AllSequenceComponents                  extends ShutdownSequenceComponentPolicy
}
