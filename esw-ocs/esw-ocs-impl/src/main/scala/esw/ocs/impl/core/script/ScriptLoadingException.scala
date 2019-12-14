package esw.ocs.impl.core.script

import csw.prefix.models.Subsystem

object ScriptLoadingException {
  class ScriptConfigurationMissingException(subsystem: Subsystem, observingMode: String)
      extends RuntimeException(s"Script configuration missing for [${subsystem.name}] with [$observingMode]")

  class InvalidScriptException(scriptClass: String) extends RuntimeException(s"$scriptClass should be subclass of Script")

  class ScriptNotFound(scriptClass: String) extends RuntimeException(s"$scriptClass not found at configured path")

}
