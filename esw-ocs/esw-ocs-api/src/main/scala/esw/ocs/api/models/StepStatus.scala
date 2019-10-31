package esw.ocs.api.models

import csw.params.commands.CommandResponse.SubmitResponse

sealed trait StepStatus

object StepStatus {
  case object Pending   extends StepStatus
  case object InFlight  extends StepStatus
  sealed trait Finished extends StepStatus
  object Finished {
    case class Success(response: SubmitResponse) extends Finished
    case class Failure(error: SubmitResponse)    extends Finished
  }
}
