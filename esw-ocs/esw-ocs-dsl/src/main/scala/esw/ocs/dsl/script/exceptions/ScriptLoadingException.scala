package esw.ocs.dsl.script.exceptions

object ScriptLoadingException {
  class ScriptConfigurationMissingException(packageId: String, observingMode: String)
      extends RuntimeException(s"Script configuration missing for $packageId with $observingMode")

  class InvalidScriptException(scriptClass: String) extends RuntimeException(s"$scriptClass should be subclass of Script")

  class ScriptNotFound(scriptClass: String) extends RuntimeException(s"$scriptClass not found at configured path")

  class ScriptInitialisationFailedException(msg: String) extends RuntimeException(s"Script initialization failed with : $msg")
}
