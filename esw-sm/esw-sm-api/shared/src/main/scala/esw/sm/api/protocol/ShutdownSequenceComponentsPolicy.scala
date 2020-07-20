package esw.sm.api.protocol

import csw.prefix.models.Prefix

sealed trait ShutdownSequenceComponentsPolicy

object ShutdownSequenceComponentsPolicy {
  case class SingleSequenceComponent(prefix: Prefix) extends ShutdownSequenceComponentsPolicy
  case object AllSequenceComponents                  extends ShutdownSequenceComponentsPolicy
}
