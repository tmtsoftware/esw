package esw.sm.impl.core

import java.nio.file.Paths

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.config.client.commons.ConfigUtils
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem._
import esw.ocs.api.models.ObsMode
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

  private val configUtils                 = mock[ConfigUtils]
  private val sequenceManagerConfigParser = new SequenceManagerConfigParser(configUtils)

  override protected def afterEach(): Unit = {
    reset(configUtils)
    super.afterEach()
  }

  "readObsModeConfig" must {
    "read obs mode config from local file | ESW-162" in {
      val path                            = Paths.get("testObsModeConfig.conf")
      val darkNightSequencers: Sequencers = Sequencers(IRIS, ESW, TCS, AOESW)
      val calSequencers: Sequencers       = Sequencers(IRIS, ESW, AOESW)
      val testConfig                      = ConfigFactory.parseResources(path.getFileName.toString)

      when(configUtils.getConfig(inputFilePath = path, isLocal = true)).thenReturn(Future.successful(testConfig))

      val config = sequenceManagerConfigParser.readObsModeConfig(configFilePath = path, isLocal = true)

      val expectedConfig = SequenceManagerConfig(
        Map(
          ObsMode("IRIS_DarkNight") -> ObsModeConfig(Resources(iris, tcs, nfiraos), darkNightSequencers),
          ObsMode("IRIS_Cal")       -> ObsModeConfig(Resources(iris, nscu, nfiraos), calSequencers)
        )
      )
      config.futureValue should ===(expectedConfig)
    }

    "throw exception if config file has invalid obsMode config structure | ESW-162, ESW-160" in {
      val path       = Paths.get("invalidTestConfig.conf")
      val testConfig = ConfigFactory.parseResources(path.getFileName.toString)
      when(configUtils.getConfig(inputFilePath = path, isLocal = true)).thenReturn(Future.successful(testConfig))

      intercept[InvalidInputData[Any]](
        sequenceManagerConfigParser.readObsModeConfig(isLocal = true, configFilePath = path).awaitResult
      )
    }
  }

  "readProvisionConfig" must {
    "read provision config from given file | ESW-346" in {
      val path       = Paths.get("testProvisionConfig.conf")
      val testConfig = ConfigFactory.parseResources(path.getFileName.toString)
      when(configUtils.getConfig(inputFilePath = path, isLocal = true)).thenReturn(Future.successful(testConfig))

      val config = sequenceManagerConfigParser.readProvisionConfig(configFilePath = path, isLocal = true)

      config.futureValue should ===(ProvisionConfig(Map(ESW -> 3, IRIS -> 2, TCS -> 1)))
    }

    "throw exception if config file has invalid provision config structure | ESW-346" in {
      val path       = Paths.get("invalidProvisionConfig.conf")
      val testConfig = ConfigFactory.parseResources(path.getFileName.toString)
      when(configUtils.getConfig(inputFilePath = path, isLocal = true)).thenReturn(Future.successful(testConfig))

      intercept[InvalidInputData[Any]](
        sequenceManagerConfigParser.readProvisionConfig(configFilePath = path, isLocal = true).awaitResult
      )
    }
  }

  "readObsModeConfig and readProvisionConfig must throw exception if it fails to read config | ESW-162, ESW-160, ESW-346" in {
    val path              = Paths.get("testObsModeConfig.conf")
    val expectedException = new RuntimeException("Failed to read config")

    // config server getConfig fails with exception
    when(configUtils.getConfig(inputFilePath = path, isLocal = true)).thenReturn(Future.failed(expectedException))

    val obsModeConfigException = intercept[RuntimeException](
      sequenceManagerConfigParser.readObsModeConfig(isLocal = true, configFilePath = path).awaitResult
    )

    val provisionConfigException = intercept[RuntimeException](
      sequenceManagerConfigParser.readProvisionConfig(isLocal = true, configFilePath = path).awaitResult
    )

    obsModeConfigException should ===(expectedException)
    provisionConfigException should ===(expectedException)
  }

  implicit class FutureOps[T](f: Future[T]) {
    def awaitResult: T = Await.result(f, defaultTimeout)
  }
}
