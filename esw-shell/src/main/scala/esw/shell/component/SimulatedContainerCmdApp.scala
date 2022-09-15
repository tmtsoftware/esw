/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.shell.component

import csw.framework.deploy.containercmd.ContainerCmd
import csw.prefix.models.Subsystem.ESW

//Main app for simulated container
object SimulatedContainerCmdApp extends App {
  ContainerCmd.start("simulated_container_cmd_app", ESW, args)
}
