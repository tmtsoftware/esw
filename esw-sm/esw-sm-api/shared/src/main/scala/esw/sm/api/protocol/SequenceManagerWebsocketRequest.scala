package esw.sm.api.protocol

import csw.prefix.models.Subsystem

sealed trait SequenceManagerWebsocketRequest

object SequenceManagerWebsocketRequest {

  case class Configure(obsMode: String)                                     extends SequenceManagerWebsocketRequest
  case class Cleanup(observingMode: String)                                 extends SequenceManagerWebsocketRequest
  case class StartSequencer(subsystem: Subsystem, observingMode: String)    extends SequenceManagerWebsocketRequest
  case class ShutdownSequencer(subsystem: Subsystem, observingMode: String) extends SequenceManagerWebsocketRequest
  case class RestartSequencer(subsystem: Subsystem, observingMode: String)  extends SequenceManagerWebsocketRequest
}
