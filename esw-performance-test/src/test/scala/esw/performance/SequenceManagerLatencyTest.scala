package esw.performance

import java.io.{File, FileOutputStream, PrintStream}

import akka.actor.typed.{ActorSystem, SpawnProtocol}
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
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.client.SequenceManagerApiFactory
import esw.sm.api.models.{AgentProvisionConfig, ProvisionConfig}
import esw.sm.api.protocol.ProvisionResponse
import org.HdrHistogram.Histogram

import scala.concurrent.duration.DurationInt

class SequenceManagerLatencyTest extends LocationUtils {

  override def locationService: LocationService                         = HttpLocationServiceFactory.makeLocalClient
  override implicit def actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "perf-test")
  override implicit def patienceConfig: PatienceConfig                  = PatienceConfig(120.seconds, 50.millis)

  LocationServerStatus.requireUpLocally()
  val loggerFactory = new LoggerFactory(Prefix(ESW, "perf.test"))
  val log: Logger   = loggerFactory.getLogger
  val histogram     = new Histogram(3)

  "Sequence Manager" must {
    "configure observing mode with permissible latency | ESW-175" in {
      val sequenceManagerPrefix = Prefix(ESW, "sequence_manager")
      val smLocation            = resolveAkkaLocation(sequenceManagerPrefix, Service)
      val smClient              = SequenceManagerApiFactory.makeAkkaClient(smLocation)

      val provisionConfig = ProvisionConfig(
        List(
          AgentProvisionConfig(Prefix("ESW.machine1"), 1),
          AgentProvisionConfig(Prefix("IRIS.machine1"), 1),
          AgentProvisionConfig(Prefix("TCS.machine1"), 1),
          AgentProvisionConfig(Prefix("AOESW.machine1"), 1),
          AgentProvisionConfig(Prefix("WFOS.machine1"), 1)
        )
      )

      val provisionResponse = smClient.provision(provisionConfig).futureValue
      println(s"---------> ${provisionResponse}")
      provisionResponse shouldBe a[ProvisionResponse.Success.type]

      val deadline = 20.seconds.fromNow
      while (deadline.hasTimeLeft()) {
        scenario(smClient)
      }

      recordResults()

      actorSystem.terminate()
    }
  }

  private def recordResults(): Unit = {
    try {
      val resultsFile = new File("~/results.txt")
      val fout        = new FileOutputStream(resultsFile)
      try histogram.outputPercentileDistribution(new PrintStream(fout), 1.0)
      finally if (fout != null) fout.close()
    }
    catch {
      case e: Exception => log.error("Writing histogram results failed with error", ex = e)
    }
  }

  private def scenario(smClient: SequenceManagerApi): Unit = {
    val obsmode1 = ObsMode("obsMode1")
    val obsmode2 = ObsMode("obsMode2")
    val obsmode3 = ObsMode("obsMode3")

    //-------------------- Configure Obsmode1 -------------------------------
    val configureResponse1 = smClient.configure(obsmode1).futureValue
    log.info(s"Configured ObsMode ${obsmode1} response ${configureResponse1}")
    println(s"Configured ObsMode ${obsmode1} response ${configureResponse1}")

    // To simulate observation
    Thread.sleep(500) //todo: async delay

    val time0 = System.currentTimeMillis()
    smClient.shutdownObsModeSequencers(obsmode1).futureValue
    val configureResponse2 = smClient.configure(obsmode2).futureValue
    val time1              = System.currentTimeMillis()
    val diff0              = time1 - time0
    histogram.recordValue(diff0)
    log.info(s"Configured ObsMode ${obsmode2} response ${configureResponse2}")
    println(s"Configured ObsMode ${obsmode2} response ${configureResponse2}")
    println("Shutdown + configure time: " + diff0)

    // To simulate observation
    Thread.sleep(500) //todo: async delay

    val time2 = System.currentTimeMillis()
    smClient.shutdownObsModeSequencers(obsmode2).futureValue
    val configureResponse3 = smClient.configure(obsmode3).futureValue
    val time3              = System.currentTimeMillis()
    val diff1              = time3 - time2
    histogram.recordValue(diff1)
    log.info(s"Configured ObsMode ${obsmode3} response ${configureResponse3}")
    println(s"Configured ObsMode ${obsmode3} response ${configureResponse3}")
    println("Shutdown + configure time: " + diff1)

    Thread.sleep(500) //todo: async delay

    val time4 = System.currentTimeMillis()
    smClient.shutdownObsModeSequencers(obsmode3).futureValue
    val configureResponse4 = smClient.configure(obsmode1).futureValue
    val time5              = System.currentTimeMillis()
    val diff2              = time5 - time4
    histogram.recordValue(diff2)
    log.info(s"Configured ObsMode ${obsmode1} response ${configureResponse4}")
    println(s"Configured ObsMode ${obsmode1} response ${configureResponse4}")
    println("Shutdown + configure time: " + diff2)

    // To simulate observation
    Thread.sleep(500) //todo: async delay

    smClient.shutdownObsModeSequencers(obsmode1).futureValue

  }
}
