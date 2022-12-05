package esw.agent.test

import csw.framework.deploy.containercmd.ContainerCmd
import csw.prefix.models.Subsystem.ESW

object TestContainerCmdApp {
  def main(args: Array[String]): Unit = {
    ContainerCmd.start("test_container_cmd_app", ESW, args)
  }
}
