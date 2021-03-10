package esw.shell.component

import csw.framework.deploy.containercmd.ContainerCmd
import csw.prefix.models.Subsystem.ESW

object SimulatedContainerCmdApp extends App {
  ContainerCmd.start("simulated_container_cmd_app", ESW, args)
}
