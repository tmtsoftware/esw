package esw.ocs.framework.api.models

sealed trait StepStatus extends Product with Serializable

object StepStatus {
  case object Pending  extends StepStatus
  case object InFlight extends StepStatus
  case object Finished extends StepStatus
}
