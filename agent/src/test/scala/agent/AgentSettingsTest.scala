package agent

import java.nio.file.Paths

import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.duration.DurationLong

class AgentSettingsTest extends WordSpecLike with Matchers {

  "binariesPath" must {
    "return the same as given path when path is absolute | ESW-237" in {
      val agentSettings = AgentSettings("/binaries", 2.seconds)
      agentSettings.binariesPath.toString should ===("/binaries")
    }
    "return absolute path when given path is relative to home directory | ESW-237" in {
      val agentSettings = AgentSettings("~/binaries", 2.seconds)
      val homeDir       = System.getProperty("user.home")
      agentSettings.binariesPath should ===(Paths.get(homeDir, "binaries"))
    }
    "throw runtime exception when a relative path is given" in {
      val ex = intercept[RuntimeException](AgentSettings("binaries", 2.seconds))
      ex.getMessage should ===("binariesPath should be absolute path")
    }
  }
}
