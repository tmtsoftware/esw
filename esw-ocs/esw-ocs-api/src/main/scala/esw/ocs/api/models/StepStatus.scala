package esw.ocs.api.models

sealed trait StepStatus

object StepStatus {
  case object Pending   extends StepStatus
  case object InFlight  extends StepStatus
  sealed trait Finished extends StepStatus
  object Finished {
    case object Success                 extends Finished
    case class Failure(message: String) extends Finished
  }
}
