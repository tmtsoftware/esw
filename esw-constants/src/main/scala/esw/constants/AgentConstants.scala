package esw.constants

import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.CSW

object AgentConstants {
  val EventPrefix: Prefix    = Prefix(CSW, "EventServer")
  val AlarmPrefix: Prefix    = Prefix(CSW, "AlarmServer")
  val DatabasePrefix: Prefix = Prefix(CSW, "DatabaseServer")
}
