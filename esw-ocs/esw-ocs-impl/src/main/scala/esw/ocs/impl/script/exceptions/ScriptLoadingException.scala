package esw.ocs.impl.script.exceptions

import csw.prefix.models.Subsystem

object ScriptLoadingException {
  class ScriptConfigurationMissingException(subsystem: Subsystem, componentName: String)
      extends RuntimeException(s"Script configuration missing for [${subsystem.name}] with [$componentName]")

  class InvalidScriptException(scriptFile: String) extends RuntimeException(s"$scriptFile should contain a subclass of Script")

  class ScriptNotFound(scriptFile: String) extends RuntimeException(s"$scriptFile not found at configured path")

}
