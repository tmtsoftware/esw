package esw.sm.impl.core

import java.nio.file.Paths

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.config.client.commons.ConfigUtils
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem._
import esw.sm.impl.config._
import esw.testcommons.BaseTestSuite
import io.bullet.borer.Borer.Error.InvalidInputData

import scala.concurrent.{Await, ExecutionContext, Future}

class SequenceManagerConfigParserTest extends BaseTestSuite {
  private val actorSystem                   = ActorSystem(SpawnProtocol(), "test-system")
  implicit private val ec: ExecutionContext = actorSystem.executionContext

  private val iris: Resource    = Resource(IRIS)
  private val tcs: Resource     = Resource(TCS)
  private val nfiraos: Resource = Resource(NFIRAOS)
  private val nscu: Resource    = Resource(Subsystem.NSCU)

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
          "IRIS_Darknight" -> ObsModeConfig(Resources(iris, tcs, nfiraos), darknightSequencers),
          "IRIS_Cal"       -> ObsModeConfig(Resources(iris, nscu, nfiraos), calSequencers)
        ),
        sequencerStartRetries = 2
      )
      config.futureValue should ===(expectedConfig)
    }

    "throw exception if config file has invalid config structure | ESW-162, ESW-160" in {
      val configUtils                 = mock[ConfigUtils]
      val path                        = Paths.get("invalidTestConfig.conf")
      val sequenceManagerConfigParser = new SequenceManagerConfigParser(configUtils)
      val testConfig                  = ConfigFactory.parseResources("invalidTestConfig.conf")
      when(configUtils.getConfig(inputFilePath = path, isLocal = true)).thenReturn(Future.successful(testConfig))

      intercept[InvalidInputData[Any]](
        sequenceManagerConfigParser.read(isLocal = true, configFilePath = path).awaitResult
      )
    }

    "throw exception if it fails to read config | ESW-162, ESW-160" in {
      val configUtils                 = mock[ConfigUtils]
      val path                        = Paths.get("testConfig.conf")
      val sequenceManagerConfigParser = new SequenceManagerConfigParser(configUtils)
      val expectedException           = new RuntimeException("Failed to read config")

      // config server getConfig fails with exception
      when(configUtils.getConfig(inputFilePath = path, isLocal = true)).thenReturn(Future.failed(expectedException))

      val exception = intercept[RuntimeException](
        sequenceManagerConfigParser.read(isLocal = true, configFilePath = path).awaitResult
      )

      exception should ===(expectedException)
    }

    "read sequencer start retires config from application.conf if not present in main config file | ESW-176" in {
      val configUtils                     = mock[ConfigUtils]
      val path                            = Paths.get("testConfigWithoutRetries.conf")
      val sequenceManagerConfigParser     = new SequenceManagerConfigParser(configUtils)
      val darknightSequencers: Sequencers = Sequencers(IRIS, ESW, TCS, AOESW)
      val calSequencers: Sequencers       = Sequencers(IRIS, ESW, AOESW)
      val testConfig                      = ConfigFactory.parseResources("testConfigWithoutRetries.conf")
      when(configUtils.getConfig(inputFilePath = path, isLocal = true)).thenReturn(Future.successful(testConfig))

      val config = sequenceManagerConfigParser.read(configFilePath = path, isLocal = true)

      val expectedConfig = SequenceManagerConfig(
        Map(
          "IRIS_Darknight" -> ObsModeConfig(Resources(iris, tcs, nfiraos), darknightSequencers),
          "IRIS_Cal"       -> ObsModeConfig(Resources(iris, nscu, nfiraos), calSequencers)
        ),
        sequencerStartRetries = 3
      )
      config.futureValue should ===(expectedConfig)
    }
  }

  implicit class FutureOps[T](f: Future[T]) {
    def awaitResult: T = Await.result(f, defaultTimeout)
  }
}
