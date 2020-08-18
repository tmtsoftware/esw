package esw.agent.akka.app

import com.typesafe.config.ConfigFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.testcommons.BaseTestSuite

import scala.concurrent.duration.DurationLong

class AgentSettingsTest extends BaseTestSuite {

  "from" must {
    "create new AgentSettings from config object | ESW-237, ESW-366" in {
      val config =
        ConfigFactory.parseString(s"""
          |agent {
          |  durationToWaitForComponentRegistration = 15s
          |  coursier.channel = "${Cs.channel}"
          |}
          |""".stripMargin)

      val prefix        = Prefix(ESW, "machine_A1")
      val agentSettings = AgentSettings(prefix, config)
      agentSettings should ===(AgentSettings(prefix, 15.seconds, Cs.channel))
    }
  }
}
