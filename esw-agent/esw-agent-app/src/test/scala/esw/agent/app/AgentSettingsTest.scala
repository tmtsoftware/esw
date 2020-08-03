package esw.agent.app

import com.typesafe.config.ConfigFactory
import esw.testcommons.BaseTestSuite

import scala.concurrent.duration.DurationLong

class AgentSettingsTest extends BaseTestSuite {

  "from" must {
    "create new AgentSettings from config object | ESW-237" in {
      val config =
        ConfigFactory.parseString(s"""
          |agent {
          |  durationToWaitForComponentRegistration = 15s
          |  coursier.channel = "${Cs.channel}"
          |}
          |""".stripMargin)
      val agentSettings = AgentSettings.from(config)
      agentSettings should ===(new AgentSettings(15.seconds, Cs.channel))
    }
  }
}
