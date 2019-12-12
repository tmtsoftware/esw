package esw.ocs.dsl.script.exceptions
import csw.params.core.models.Subsystem

object ScriptLoadingException {
  class ScriptConfigurationMissingException(subsystem: Subsystem, observingMode: String)
      extends RuntimeException(s"Script configuration missing for [${subsystem.name}] with [$observingMode]")

  class InvalidScriptException(scriptClass: String) extends RuntimeException(s"$scriptClass should be subclass of Script")

  class ScriptNotFound(scriptClass: String) extends RuntimeException(s"$scriptClass not found at configured path")

  class ScriptInitialisationFailedException(msg: String) extends RuntimeException(s"Script initialization failed with : $msg")
}
