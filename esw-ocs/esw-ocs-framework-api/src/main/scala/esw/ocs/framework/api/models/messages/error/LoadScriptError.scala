package esw.ocs.framework.api.models.messages.error

final case class LoadScriptError private (msg: String)

object LoadScriptError {
  def apply(reason: String): LoadScriptError = new LoadScriptError(s"Loading script Failed: $reason")
}
