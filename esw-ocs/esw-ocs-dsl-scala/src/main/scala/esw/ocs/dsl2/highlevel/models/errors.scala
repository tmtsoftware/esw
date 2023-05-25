package esw.ocs.dsl2.highlevel.models

import csw.params.commands.CommandResponse
import csw.params.commands.CommandResponse.SubmitResponse

sealed abstract class ScriptError(val cause: Option[Throwable] = None) extends RuntimeException(cause.orNull) {
  def reason: String
  def message: Option[String] = Some(reason)
}

case class CommandError(submitResponse: SubmitResponse) extends ScriptError() {
  override val reason: String = submitResponse.toString
}

case class OtherError(reason: String, override val cause: Option[Throwable] = None) extends ScriptError(cause)
