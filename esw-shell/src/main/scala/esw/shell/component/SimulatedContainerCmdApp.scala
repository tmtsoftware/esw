package esw.shell.component

import csw.framework.deploy.containercmd.ContainerCmd
import csw.prefix.models.Subsystem.ESW

//Main app for simulated container
object SimulatedContainerCmdApp {
  def main(args: Array[String]): Unit = {
    ContainerCmd.start("simulated_container_cmd_app", ESW, args)
  }
}
