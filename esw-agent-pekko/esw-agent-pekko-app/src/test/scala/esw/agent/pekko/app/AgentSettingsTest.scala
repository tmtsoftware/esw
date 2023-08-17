package esw.agent.pekko.app

import java.nio.file.Path

import com.typesafe.config.ConfigFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.testcommons.BaseTestSuite

class AgentSettingsTest extends BaseTestSuite {

  "from" must {
    "create new AgentSettings from config object | ESW-237, ESW-366" in {
      val config =
        ConfigFactory.parseString(s"""
          |agent {
          |  coursier.channel = "${Cs.channel}"
          |  osw.version.confPath = "script/resources/application.conf"
          |}
          |""".stripMargin)

      val prefix        = Prefix(ESW, "machine_A1")
      val agentSettings = AgentSettings(prefix, config)
      agentSettings should ===(AgentSettings(prefix, Cs.channel, Path.of("script/resources/application.conf")))
    }
  }
}
