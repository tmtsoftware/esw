package esw.sm.api.protocol

import csw.prefix.models.Subsystem

sealed trait SequenceManagerPostRequest
object SequenceManagerPostRequest {
  case object GetRunningObsModes                                            extends SequenceManagerPostRequest
  case class Cleanup(observingMode: String)                                 extends SequenceManagerPostRequest
  case class StartSequencer(subsystem: Subsystem, observingMode: String)    extends SequenceManagerPostRequest
  case class ShutdownSequencer(subsystem: Subsystem, observingMode: String) extends SequenceManagerPostRequest
  case class RestartSequencer(subsystem: Subsystem, observingMode: String)  extends SequenceManagerPostRequest
}
