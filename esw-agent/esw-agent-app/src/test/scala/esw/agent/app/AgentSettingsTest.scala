package esw.agent.app

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationLong

class AgentSettingsTest extends AnyWordSpecLike with Matchers {

  "from" must {
    "create new AgentSettings from config object | ESW-237" in {
      val config =
        ConfigFactory.parseString(s"""
          |agent {
          |  durationToWaitForComponentRegistration = 15s
          |  durationToWaitForGracefulProcessTermination = 10s
          |  coursier.channel = "${Cs.channel}"
          |}
          |""".stripMargin)
      val agentSettings = AgentSettings.from(config)
      agentSettings should ===(new AgentSettings(15.seconds, 10.seconds, Cs.channel))
    }
  }
}
