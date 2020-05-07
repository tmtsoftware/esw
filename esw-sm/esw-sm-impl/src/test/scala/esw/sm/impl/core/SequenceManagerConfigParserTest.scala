package esw.sm.impl.core

import java.nio.file.Paths

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.{ConfigException, ConfigFactory}
import csw.config.client.commons.ConfigUtils
import csw.prefix.models.Subsystem._
import esw.commons.BaseTestSuite
import esw.sm.api.models.{ObsModeConfig, Resources, SequenceManagerConfig, Sequencers}
import io.bullet.borer.Borer.Error.InvalidInputData

import scala.concurrent.{ExecutionContext, Future}

class SequenceManagerConfigParserTest extends BaseTestSuite {
  private val actorSystem                   = ActorSystem(SpawnProtocol(), "test-system")
  implicit private val ec: ExecutionContext = actorSystem.executionContext

  "read" must {
    "read config from local file | ESW-162" in {
      val configUtils                     = mock[ConfigUtils]
      val path                            = Paths.get("testConfig.conf")
      val sequenceManagerConfigParser     = new SequenceManagerConfigParser(configUtils)
      val darknightSequencers: Sequencers = Sequencers(IRIS, ESW, TCS, AOESW)
      val calSequencers: Sequencers       = Sequencers(IRIS, ESW, AOESW)
      val testConfig                      = ConfigFactory.parseResources("testConfig.conf")
      when(configUtils.getConfig(inputFilePath = path, isLocal = true)).thenReturn(Future.successful(testConfig))

      val config = sequenceManagerConfigParser.read(configFilePath = path, isLocal = true)

      val expectedConfig = SequenceManagerConfig(
        Map(
          "IRIS_Darknight" -> ObsModeConfig(Resources("IRIS", "TCS", "NFIRAOS"), darknightSequencers),
          "IRIS_Cal"       -> ObsModeConfig(Resources("IRIS", "NCSU", "NFIRAOS"), calSequencers)
        )
      )
      config.awaitResult shouldBe expectedConfig
    }

    "throw exception if config file has missing obsMode key | ESW-162" in {
      val configUtils                 = mock[ConfigUtils]
      val path                        = Paths.get("missingTestConfig.conf")
      val sequenceManagerConfigParser = new SequenceManagerConfigParser(configUtils)
      val testConfig                  = ConfigFactory.parseResources("missingTestConfig.conf")
      when(configUtils.getConfig(inputFilePath = path, isLocal = true)).thenReturn(Future.successful(testConfig))

      intercept[ConfigException.Missing](
        sequenceManagerConfigParser.read(isLocal = true, configFilePath = path).awaitResult
      )
    }

    "throw exception if config file has invalid config structure | ESW-162" in {
      val configUtils                 = mock[ConfigUtils]
      val path                        = Paths.get("invalidTestConfig.conf")
      val sequenceManagerConfigParser = new SequenceManagerConfigParser(configUtils)
      val testConfig                  = ConfigFactory.parseResources("invalidTestConfig.conf")
      when(configUtils.getConfig(inputFilePath = path, isLocal = true)).thenReturn(Future.successful(testConfig))

      intercept[InvalidInputData[Any]](
        sequenceManagerConfigParser.read(isLocal = true, configFilePath = path).awaitResult
      )
    }

    "throw exception if it fails to read config | ESW-162" in {
      val configUtils                 = mock[ConfigUtils]
      val path                        = Paths.get("testConfig.conf")
      val sequenceManagerConfigParser = new SequenceManagerConfigParser(configUtils)
      val expectedException           = new RuntimeException("Failed to read config")

      // config server getConfig fails with exception
      when(configUtils.getConfig(inputFilePath = path, isLocal = true)).thenReturn(Future.failed(expectedException))

      val exception = intercept[RuntimeException](
        sequenceManagerConfigParser.read(isLocal = true, configFilePath = path).awaitResult
      )

      exception shouldBe expectedException
    }
  }
}
