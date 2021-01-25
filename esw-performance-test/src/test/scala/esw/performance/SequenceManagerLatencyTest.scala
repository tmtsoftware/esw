package esw.performance

import java.io.{File, FileOutputStream, PrintStream}

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
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.client.SequenceManagerApiFactory
import esw.sm.api.protocol.ProvisionResponse
import org.HdrHistogram.Histogram

import scala.concurrent.duration.DurationInt

class SequenceManagerLatencyTest extends LocationUtils {

  override def locationService: LocationService                         = HttpLocationServiceFactory.makeLocalClient
  override implicit def actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "perf-test")
  override implicit def patienceConfig: PatienceConfig                  = PatienceConfig(120.seconds, 50.millis)

  LocationServerStatus.requireUpLocally()
  val loggerFactory                 = new LoggerFactory(Prefix(ESW, "perf.test"))
  val sequenceManagerPrefix: Prefix = Prefix(ESW, "sequence_manager")
  val smLocation: AkkaLocation      = resolveAkkaLocation(sequenceManagerPrefix, Service)
  val smClient: SequenceManagerApi  = SequenceManagerApiFactory.makeAkkaClient(smLocation)

  val log: Logger = loggerFactory.getLogger
  "Sequence Manager" must {
    "configure observing mode with permissible latency | ESW-175" in {

      val histogram       = new Histogram(3)
      val warmUpHistogram = new Histogram(3)

      val provisionResponse = smClient.provision(provisionConfig).futureValue
      provisionResponse shouldBe a[ProvisionResponse.Success.type]

      (1 to warmupIterations).foreach { iterationNumber =>
        println(s"Warmup iteration ------> $iterationNumber")
        scenario(warmUpHistogram)
      }

      println("Warmup Latencies------>")
      printResults(warmUpHistogram)

      (1 to actualIterations).foreach { iterationNumber =>
        println(s"Actual iteration ------> $iterationNumber")
        scenario(histogram)
      }

      recordResults(histogram)
      println("Actual Latencies------>")
      printResults(histogram)

      actorSystem.terminate()
    }
  }

  private def printResults(histogram: Histogram): Unit = {
    println("50 %tile: " + histogram.getValueAtPercentile(50))
    println("90 %tile: " + histogram.getValueAtPercentile(90))
    println("99 %tile: " + histogram.getValueAtPercentile(99))
  }

  private def recordResults(histogram: Histogram): Unit = {
    try {
      val resultsFile = new File("results.txt")
      resultsFile.createNewFile()
      println(resultsFile.getAbsolutePath)
      val fout = new FileOutputStream(resultsFile)
      try histogram.outputPercentileDistribution(new PrintStream(fout), 1.0)
      finally if (fout != null) fout.close()
    }
    catch {
      case e: Exception =>
        println("Writing histogram results failed with error " + e.getMessage)
        log.error("Writing histogram results failed with error", ex = e)

    }
  }

  private def scenario(histogram: Histogram): Unit = {
    //-------------------- Configure Obsmode1 -------------------------------
    val configureResponse1 = smClient.configure(obsmode1).futureValue
    log.info(s"Configured ObsMode $obsmode1 response $configureResponse1")
    println(s"Configured ObsMode $obsmode1 response $configureResponse1")

    Thread.sleep(60 * 1000)

    if (enableSwitching) {
      switchObsMode(obsmode1, obsmode2, histogram)
      switchObsMode(obsmode2, obsmode3, histogram)
      switchObsMode(obsmode3, obsmode1, histogram)
    }

    smClient.shutdownObsModeSequencers(obsmode1).futureValue

  }

  private def switchObsMode(prevObsMode: ObsMode, nextObsMode: ObsMode, histogram: Histogram): Unit = {
    val beforeSwitch = System.currentTimeMillis()
    smClient.shutdownObsModeSequencers(prevObsMode).futureValue
    val configureResponse = smClient.configure(nextObsMode).futureValue
    val afterSwitch       = System.currentTimeMillis()

    val latency = afterSwitch - beforeSwitch
    histogram.recordValue(latency)
    log.info(s"Configured ObsMode $nextObsMode response $configureResponse")
    println(s"Configured ObsMode $nextObsMode response $configureResponse")
    println("Shutdown + configure time: " + latency)

    // To simulate observation
    Thread.sleep(60 * 1000) //todo: async delay
  }
}
