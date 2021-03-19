package esw.sm.api.protocol

import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode
import esw.sm.api.models.ProvisionConfig

sealed trait SequenceManagerRequest

object SequenceManagerRequest {
  case class Configure(obsMode: ObsMode)        extends SequenceManagerRequest
  case class Provision(config: ProvisionConfig) extends SequenceManagerRequest
  case object GetObsModesDetails                extends SequenceManagerRequest

  case class StartSequencer(subsystem: Subsystem, obsMode: ObsMode)   extends SequenceManagerRequest
  case class RestartSequencer(subsystem: Subsystem, obsMode: ObsMode) extends SequenceManagerRequest

  // Shutdown sequencers
  case class ShutdownSequencer(subsystem: Subsystem, obsMode: ObsMode) extends SequenceManagerRequest
  case class ShutdownSubsystemSequencers(subsystem: Subsystem)         extends SequenceManagerRequest
  case class ShutdownObsModeSequencers(obsMode: ObsMode)               extends SequenceManagerRequest
  case object ShutdownAllSequencers                                    extends SequenceManagerRequest

  case class ShutdownSequenceComponent(prefix: Prefix) extends SequenceManagerRequest
  case object ShutdownAllSequenceComponents            extends SequenceManagerRequest

  case object GetResources extends SequenceManagerRequest
}
