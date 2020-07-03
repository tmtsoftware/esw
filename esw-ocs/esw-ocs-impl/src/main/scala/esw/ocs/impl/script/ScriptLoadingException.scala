package esw.ocs.impl.script

import csw.prefix.models.Subsystem
import esw.ocs.api.models.ObsMode

object ScriptLoadingException {
  class ScriptConfigurationMissingException(subsystem: Subsystem, obsMode: ObsMode)
      extends RuntimeException(s"Script configuration missing for [${subsystem.name}] with [${obsMode.name}]")

  class InvalidScriptException(scriptClass: String) extends RuntimeException(s"$scriptClass should be subclass of Script")

  class ScriptNotFound(scriptClass: String) extends RuntimeException(s"$scriptClass not found at configured path")

}
