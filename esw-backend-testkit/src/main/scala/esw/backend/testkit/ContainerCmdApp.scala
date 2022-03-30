/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.backend.testkit

import csw.framework.deploy.containercmd.ContainerCmd
import csw.prefix.models.Subsystem.ESW
import esw.backend.testkit.utils.IOUtils

object ContainerCmdApp extends App {

  private val updatedArgs = args.map {
    case arg: String if arg.contains(".conf") => IOUtils.writeResourceToFile(arg).toString
    case arg                                  => arg
  }

  ContainerCmd.start("ContainerCmdApp", ESW, updatedArgs)
}
