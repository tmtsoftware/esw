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
import org.scalatest.prop.TableDrivenPropertyChecks

import scala.concurrent.{Await, ExecutionContext, Future}

class SequenceManagerConfigParserTest extends BaseTestSuite with TableDrivenPropertyChecks {
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

  "read" must {
    val args = Table(
      ("readObsModeConfigArg", "getConfigArg", "fileLocation"),
      (true, true, "local"),
      (false, false, "remote")
    )

    forAll(args) { (readObsModeConfigArg, getConfigArg, fileLocation) =>
      s"read obs mode config file from $fileLocation | ESW-162" in {
        val path                            = Paths.get("testObsModeConfig.conf")
        val darkNightSequencers: Sequencers = Sequencers(IRIS, ESW, TCS, AOESW)
        val calSequencers: Sequencers       = Sequencers(IRIS, ESW, AOESW)
        val testConfig                      = ConfigFactory.parseResources(path.getFileName.toString)

        when(configUtils.getConfig(inputFilePath = path, isLocal = getConfigArg)).thenReturn(Future.successful(testConfig))

        val config = sequenceManagerConfigParser.read(configFilePath = path, isLocal = readObsModeConfigArg)

        val expectedConfig = SequenceManagerConfig(
          Map(
            ObsMode("IRIS_DarkNight") -> ObsModeConfig(Resources(iris, tcs, nfiraos), darkNightSequencers),
            ObsMode("IRIS_Cal")       -> ObsModeConfig(Resources(iris, nscu, nfiraos), calSequencers)
          )
        )
        config.futureValue should ===(expectedConfig)
        verify(configUtils).getConfig(inputFilePath = path, isLocal = getConfigArg)
      }
    }

    "throw exception if config file has invalid obsMode config structure | ESW-162, ESW-160" in {
      val path       = Paths.get("invalidTestConfig.conf")
      val testConfig = ConfigFactory.parseResources(path.getFileName.toString)
      when(configUtils.getConfig(inputFilePath = path, isLocal = true)).thenReturn(Future.successful(testConfig))

      intercept[InvalidInputData[Any]](
        sequenceManagerConfigParser.read(isLocal = true, configFilePath = path).awaitResult
      )
    }
  }

  "throw exception if it fails to read config | ESW-162, ESW-160" in {
    val path              = Paths.get("testObsModeConfig.conf")
    val expectedException = new RuntimeException("Failed to read config")

    // config server getConfig fails with exception
    when(configUtils.getConfig(inputFilePath = path, isLocal = true)).thenReturn(Future.failed(expectedException))

    val obsModeConfigException = intercept[RuntimeException](
      sequenceManagerConfigParser.read(isLocal = true, configFilePath = path).awaitResult
    )

    obsModeConfigException should ===(expectedException)
  }

  implicit class FutureOps[T](f: Future[T]) {
    def awaitResult: T = Await.result(f, defaultTimeout)
  }
}
