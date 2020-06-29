package esw.sm.api.protocol

import csw.location.api.models.ComponentId
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode

sealed trait SequenceManagerPostRequest

object SequenceManagerPostRequest {
  case class Configure(obsMode: ObsMode)                                  extends SequenceManagerPostRequest
  case object GetRunningObsModes                                          extends SequenceManagerPostRequest
  case class Cleanup(observingMode: ObsMode)                              extends SequenceManagerPostRequest
  case class StartSequencer(subsystem: Subsystem, observingMode: ObsMode) extends SequenceManagerPostRequest
  case class ShutdownSequencer(subsystem: Subsystem, observingMode: ObsMode, shutdownSequenceComp: Boolean)
      extends SequenceManagerPostRequest
  case class RestartSequencer(subsystem: Subsystem, observingMode: ObsMode) extends SequenceManagerPostRequest
  case object ShutdownAllSequencers                                         extends SequenceManagerPostRequest
  case class SpawnSequenceComponent(machine: ComponentId, name: String)     extends SequenceManagerPostRequest
  case class ShutdownSequenceComponent(prefix: Prefix)                      extends SequenceManagerPostRequest
}
