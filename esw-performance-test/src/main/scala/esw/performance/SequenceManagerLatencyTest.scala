package esw.performance

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.AkkaLocation
import csw.location.api.models.ComponentType.Service
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.client.utils.LocationServerStatus
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.utils.LocationUtils
import esw.performance.Constants._
import esw.performance.utils.PerfUtils.{printResults, recordResults}
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.client.SequenceManagerApiFactory
import esw.sm.api.protocol.ConfigureResponse.Failure
import esw.sm.api.protocol._
import org.HdrHistogram.Histogram

import scala.concurrent.duration.DurationInt

object SequenceManagerLatencyTest extends LocationUtils {

  override def locationService: LocationService                         = HttpLocationServiceFactory.makeLocalClient
  override implicit def actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "perf-test")
  override implicit def patienceConfig: PatienceConfig                  = PatienceConfig(120.seconds, 50.millis)

  LocationServerStatus.requireUpLocally()
  val loggerFactory                 = new LoggerFactory(Prefix(ESW, "perf.test"))
  val sequenceManagerPrefix: Prefix = Prefix(ESW, "sequence_manager")
  val smLocation: AkkaLocation      = resolveAkkaLocation(sequenceManagerPrefix, Service)
  val smClient: SequenceManagerApi  = SequenceManagerApiFactory.makeAkkaClient(smLocation)

  val log: Logger = loggerFactory.getLogger

  def main(args: Array[String]): Unit = {

    val histogram       = new Histogram(3)
    val warmUpHistogram = new Histogram(3)

    val provisionResponse = smClient.provision(provisionConfig).futureValue
    provisionResponse shouldBe a[ProvisionResponse.Success.type]

    scenarioRepetition(histogram, warmUpHistogram)

    actorSystem.terminate()
  }

  private def scenarioRepetition(histogram: Histogram, warmUpHistogram: Histogram) = {

    (1 to warmupIterations).foreach { iterationNumber =>
      println(s"Actual iteration ------> $iterationNumber")
      scenario(warmUpHistogram)
    }

    log.info("Warmup latencies")
    printResults(warmUpHistogram)

    (1 to actualIterations).foreach { iterationNumber =>
      println(s"Actual iteration ------> $iterationNumber")
      scenario(histogram)
    }

    recordResults(histogram, "results.txt")
    log.info("Actual Latencies")
    printResults(histogram)
  }

  private def scenario(histogram: Histogram): Unit = {
    //-------------------- Configure Obsmode1 -------------------------------
    val configureResponse1 = smClient.configure(obsmode1).futureValue

    configureResponse1 match {
      case ConfigureResponse.Success(masterSequencerComponentId) =>
        println(s"Configure $obsmode1 Response --> ConfigureResponse.Success $masterSequencerComponentId")
      case failure: Failure => println(Console.RED + s"${failure.getMessage}")
    }

    Thread.sleep(Constants.timeout)

    if (enableSwitching) {
      switchObsMode(obsmode1, obsmode2, histogram)
      switchObsMode(obsmode2, obsmode3, histogram)
      switchObsMode(obsmode3, obsmode1, histogram)
    }

    val shutdownResponse = smClient.shutdownObsModeSequencers(obsmode1).futureValue
    shutdownResponse match {
      case ShutdownSequencersResponse.Success =>
        println(s"Shutdown $obsmode1 Response --> ShutdownSequencersResponse.Success $obsmode1")
      case failure: ShutdownSequencersResponse.Failure => println(Console.RED + s"${failure.getMessage}")
    }
  }

  private def switchObsMode(prevObsMode: ObsMode, nextObsMode: ObsMode, histogram: Histogram): Unit = {
    val beforeSwitch     = System.currentTimeMillis()
    val shutdownResponse = smClient.shutdownObsModeSequencers(prevObsMode).futureValue
    shutdownResponse match {
      case ShutdownSequencersResponse.Success =>
        println(s"Shutdown $prevObsMode Response --> ShutdownSequencersResponse.Success $prevObsMode")
      case failure: ShutdownSequencersResponse.Failure => println(Console.RED + s"${failure.getMessage}")
    }

    val configureResponse = smClient.configure(nextObsMode).futureValue
    configureResponse match {
      case ConfigureResponse.Success(masterSequencerComponentId) =>
        println(s"Configure $nextObsMode Response --> ConfigureResponse.Success $masterSequencerComponentId")
      case failure: Failure => println(Console.RED + s"${failure.getMessage}")
    }
    val afterSwitch = System.currentTimeMillis()

    val latency = afterSwitch - beforeSwitch
    histogram.recordValue(latency)
    log.info(s"Configure ObsMode $nextObsMode response : $configureResponse")
    log.info("latency: " + latency)

    // To simulate observation
    Thread.sleep(Constants.timeout) //todo: async delay
  }
}
