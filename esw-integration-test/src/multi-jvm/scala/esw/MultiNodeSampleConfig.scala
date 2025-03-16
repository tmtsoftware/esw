package esw

import org.apache.pekko.remote.testconductor.RoleName
import org.apache.pekko.remote.testkit.MultiNodeConfig

object MultiNodeSampleConfig extends MultiNodeConfig {
  val node1: RoleName = role("node1")
  val node2: RoleName = role("node2")
  val node3: RoleName = role("node3")
}
