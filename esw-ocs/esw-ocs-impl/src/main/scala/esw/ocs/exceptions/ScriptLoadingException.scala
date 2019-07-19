package esw.ocs.exceptions

object ScriptLoadingException {
  class ScriptConfigurationMissingException(sequencerId: String, observingMode: String)
      extends RuntimeException(s"Script configuration missing for $sequencerId with $observingMode")

  class InvalidScriptException(scriptClass: String) extends RuntimeException(s"$scriptClass should be subclass of Script")

  class ScriptNotFound(scriptClass: String) extends RuntimeException(s"$scriptClass not found at configured path")

}
