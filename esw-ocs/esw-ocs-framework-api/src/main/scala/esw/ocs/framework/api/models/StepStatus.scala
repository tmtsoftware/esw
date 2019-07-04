package esw.ocs.framework.api.models

import csw.params.commands.CommandResponse.SubmitResponse

sealed trait StepStatus extends Product with Serializable

object StepStatus {
  case object Pending   extends StepStatus
  case object InFlight  extends StepStatus
  sealed trait Finished extends StepStatus
  object Finished {
    case class Success(submitResponse: SubmitResponse) extends Finished
    case class Failure(submitResponse: SubmitResponse) extends Finished
  }
}
