package esw.sm.impl.core

import java.nio.file.Paths

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.config.client.commons.ConfigUtils
import csw.prefix.models.Subsystem._
import esw.commons.BaseTestSuite
import esw.sm.api.models.{ObsModeConfig, Resources, SequenceManagerConfig, Sequencers}

import scala.concurrent.Future

class SequenceManagerConfigParserTest extends BaseTestSuite {
  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test-system")

  "read" must {
    "read config from local file" in {
      val configUtils                     = mock[ConfigUtils]
      val path                            = Paths.get("testConfig.conf")
      val sequenceManagerConfigParser     = new SequenceManagerConfigParser(configUtils)
      val darknightSequencers: Sequencers = Sequencers(IRIS, ESW, TCS, AOESW)
      val calSequencers: Sequencers       = Sequencers(IRIS, ESW, AOESW)
      val testConfig                      = ConfigFactory.parseResources("testConfig.conf")
      when(configUtils.getConfig(true, Some(path), None)).thenReturn(Future.successful(testConfig))

      val config = sequenceManagerConfigParser.read(isLocal = true, configFilePath = Some(path), defaultConfig = None)

      val expectedConfig = SequenceManagerConfig(
        Map(
          "IRIS_Darknight" -> ObsModeConfig(Resources("IRIS", "TCS", "NFIRAOS"), darknightSequencers),
          "IRIS_cal"       -> ObsModeConfig(Resources("IRIS", "NCSU", "NFIRAOS"), calSequencers)
        )
      )
      config.awaitResult shouldBe expectedConfig
    }
  }
}
