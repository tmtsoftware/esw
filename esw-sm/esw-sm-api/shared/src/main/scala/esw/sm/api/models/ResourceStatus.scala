package esw.sm.api.models

sealed trait ResourceStatus

object ResourceStatus {

  case object InUse extends ResourceStatus

  case object Available extends ResourceStatus

  case object Idle extends ResourceStatus
}
