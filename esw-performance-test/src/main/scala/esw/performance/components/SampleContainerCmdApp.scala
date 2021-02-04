package esw.performance.components

import csw.framework.deploy.containercmd.ContainerCmd
import csw.prefix.models.Subsystem.CSW

object SampleContainerCmdApp extends App {
  ContainerCmd.start("sample_container_cmd_app", CSW, args)
}
