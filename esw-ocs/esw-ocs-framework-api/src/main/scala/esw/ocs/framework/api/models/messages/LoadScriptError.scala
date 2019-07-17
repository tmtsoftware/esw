package esw.ocs.framework.api.models.messages

case class LoadScriptError(msg: String)

object LoadScriptError {
  def apply(exception: Throwable): LoadScriptError =
    new LoadScriptError(s"Loading script Failed: ${exception.getMessage}")
}
