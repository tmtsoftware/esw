package esw.agent.app

import java.nio.file.Paths

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.DurationLong
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class AgentSettingsTest extends AnyWordSpecLike with Matchers {

  "from" must {
    "create new AgentSettings from config object | ESW-237" in {
      val config =
        ConfigFactory.parseString("""
          |agent {
          |  binariesPath = "~/Projects/tmt/esw/target/universal/stage/bin"
          |  durationToWaitForComponentRegistration = 15s
          |  durationToWaitForGracefulProcessTermination = 10s
          |  coursier.channel = "https://raw.githubusercontent.com/tmtsoftware/apps/master/apps.json"
          |}
          |""".stripMargin)
      val agentSettings = AgentSettings.from(config)
      agentSettings should ===(
        new AgentSettings(
          "~/Projects/tmt/esw/target/universal/stage/bin",
          15.seconds,
          10.seconds,
          Cs.channel
        )
      )
    }
  }

  "binariesPath" must {
    "return the same as given path when path is absolute | ESW-237" in {
      val agentSettings = AgentSettings("/binaries", 2.seconds, 2.seconds, Cs.channel)
      agentSettings.binariesPath.toString should ===("/binaries")
    }
    "return absolute path when given path is relative to home directory | ESW-237" in {
      val agentSettings = AgentSettings("~/binaries", 2.seconds, 2.seconds, Cs.channel)
      val homeDir       = System.getProperty("user.home")
      agentSettings.binariesPath should ===(Paths.get(homeDir, "binaries"))
    }
    "throw runtime exception when a relative path is given | ESW-237" in {
      val ex = intercept[RuntimeException](AgentSettings("binaries", 2.seconds, 2.seconds, Cs.channel))
      ex.getMessage should ===("binariesPath should be absolute path")
    }
  }
}
