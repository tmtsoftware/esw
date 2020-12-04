package esw.constants

import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.CSW

object AgentConstants {

  val eventPrefix: Prefix    = Prefix(CSW, "EventServer")
  val alarmPrefix: Prefix    = Prefix(CSW, "AlarmServer")
  val databasePrefix: Prefix = Prefix(CSW, "DatabaseServer")
}
