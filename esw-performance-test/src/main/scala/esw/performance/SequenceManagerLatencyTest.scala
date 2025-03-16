package esw.performance

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType.Service
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.client.utils.LocationServerStatus
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.utils.LocationUtils
import esw.performance.constants.ObsModes.*
import esw.performance.constants.SMLatencyConstants
import esw.performance.utils.PerfUtils.{printResults, recordResults}
import esw.performance.utils.Timing
import esw.sm.api.actor.client.SequenceManagerApiFactory
import esw.sm.api.protocol.ConfigureResponse.Failure
import esw.sm.api.protocol.*
import org.HdrHistogram.Histogram

import scala.concurrent.duration.DurationInt

object SequenceManagerLatencyTest extends LocationUtils {

  override def locationService: LocationService                         = HttpLocationServiceFactory.makeLocalClient
  override implicit def actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "perf-test")
  override implicit def patienceConfig: PatienceConfig                  = PatienceConfig(120.seconds, 50.millis)

  LocationServerStatus.requireUpLocally()

  private val loggerFactory         = new LoggerFactory(Prefix(ESW, "perf.test"))
  private val sequenceManagerPrefix = Prefix(ESW, "sequence_manager")
  private val smLocation            = resolvePekkoLocation(sequenceManagerPrefix, Service)
  private val smClient              = SequenceManagerApiFactory.makePekkoClient(smLocation)

  private val log = loggerFactory.getLogger

  def main(args: Array[String]): Unit = {
    val histogram       = new Histogram(3)
    val warmUpHistogram = new Histogram(3)

    val provisionResponse = smClient.provision(SMLatencyConstants.provisionConfig).futureValue
    provisionResponse shouldBe a[ProvisionResponse.Success.type]

    repeatScenario(SMLatencyConstants.warmupIterations, warmUpHistogram, "Warmup")
    repeatScenario(SMLatencyConstants.actualIterations, histogram, "Actual")
    recordResults(histogram, "results.txt")

    actorSystem.terminate()
  }

  private def repeatScenario(times: Int, histogram: Histogram, label: String): Unit = {
    (1 to times).foreach { iterationNumber =>
      println(s"$label iteration ------> $iterationNumber")
      scenario(histogram)
    }

    log.info(s"$label latencies")
    printResults(histogram)
  }

  private def scenario(histogram: Histogram): Unit = {
    // -------------------- Configure ObsMode1 -------------------------------
    configureObsMode(obsMode1)

    Thread.sleep(SMLatencyConstants.timeout)

    if (SMLatencyConstants.enableSwitching) {
      switchObsMode(obsMode1, obsMode2, histogram)
      switchObsMode(obsMode2, obsMode3, histogram)
      switchObsMode(obsMode3, obsMode1, histogram)
    }

    shutdownObsMode(obsMode1)
  }

  private def switchObsMode(prevObsMode: ObsMode, nextObsMode: ObsMode, histogram: Histogram): Unit = {
    val (configureResponse, latency) = Timing.measureTimeMillis {
      shutdownObsMode(prevObsMode)
      configureObsMode(nextObsMode)
    }
    histogram.recordValue(latency)

    log.info(s"Configure ObsMode $nextObsMode response : $configureResponse")
    log.info("latency: " + latency)

    // To simulate observation
    Thread.sleep(SMLatencyConstants.timeout) // todo: async delay
  }

  private def shutdownObsMode(obsMode: ObsMode): ShutdownSequencersResponse = {
    val shutdownResponse = smClient.shutdownObsModeSequencers(obsMode).futureValue
    shutdownResponse match {
      case ShutdownSequencersResponse.Success =>
        println(s"Shutdown $obsMode Response --> ShutdownSequencersResponse.Success $obsMode")
      case failure: ShutdownSequencersResponse.Failure => println(Console.RED + s"${failure.getMessage}")
    }
    shutdownResponse
  }

  private def configureObsMode(obsMode: ObsMode): ConfigureResponse = {
    val configureResponse = smClient.configure(obsMode).futureValue
    configureResponse match {
      case ConfigureResponse.Success(masterSequencerComponentId) =>
        println(s"Configure $obsMode Response --> ConfigureResponse.Success $masterSequencerComponentId")
      case failure: Failure => println(Console.RED + s"${failure.getMessage}")
    }
    configureResponse
  }
}
