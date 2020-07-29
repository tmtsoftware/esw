package esw.sm.api.protocol

import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode
import esw.sm.api.models.ProvisionConfig

sealed trait SequenceManagerPostRequest

object SequenceManagerPostRequest {
  case class Configure(obsMode: ObsMode)        extends SequenceManagerPostRequest
  case class Provision(config: ProvisionConfig) extends SequenceManagerPostRequest
  case object GetRunningObsModes                extends SequenceManagerPostRequest

  case class StartSequencer(subsystem: Subsystem, obsMode: ObsMode)   extends SequenceManagerPostRequest
  case class RestartSequencer(subsystem: Subsystem, obsMode: ObsMode) extends SequenceManagerPostRequest

  // Shutdown sequencers
  case class ShutdownSequencer(subsystem: Subsystem, obsMode: ObsMode) extends SequenceManagerPostRequest
  case class ShutdownSubsystemSequencers(subsystem: Subsystem)         extends SequenceManagerPostRequest
  case class ShutdownObsModeSequencers(obsMode: ObsMode)               extends SequenceManagerPostRequest
  case object ShutdownAllSequencers                                    extends SequenceManagerPostRequest

  case class SpawnSequenceComponent(machine: Prefix, name: String) extends SequenceManagerPostRequest

  case class ShutdownSequenceComponent(prefix: Prefix) extends SequenceManagerPostRequest
  case object ShutdownAllSequenceComponents            extends SequenceManagerPostRequest

  case object GetAgentStatus extends SequenceManagerPostRequest
}
