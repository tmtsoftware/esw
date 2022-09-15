/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.impl.script

import csw.prefix.models.Subsystem

object ScriptLoadingException {
  class ScriptConfigurationMissingException(subsystem: Subsystem, componentName: String)
      extends RuntimeException(s"Script configuration missing for [${subsystem.name}] with [$componentName]")

  class InvalidScriptException(scriptClass: String) extends RuntimeException(s"$scriptClass should be subclass of Script")

  class ScriptNotFound(scriptClass: String) extends RuntimeException(s"$scriptClass not found at configured path")

}
