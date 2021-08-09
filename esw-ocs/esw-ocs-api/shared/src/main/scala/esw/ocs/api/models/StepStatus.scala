package esw.ocs.api.models

/**
 * This model represent the status of a particular step - Pending, InFight etc
 */
sealed trait StepStatus

object StepStatus {
  case object Pending   extends StepStatus
  case object InFlight  extends StepStatus
  sealed trait Finished extends StepStatus
  object Finished {
    case object Success                      extends Finished
    case class Failure(message: String = "") extends Finished
  }
}
