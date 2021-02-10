package esw.performance

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType.Service
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.client.utils.LocationServerStatus
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.utils.LocationUtils
import esw.performance.Constants._
import esw.performance.utils.PerfUtils.{printResults, recordResults}
import esw.performance.utils.Timing
import esw.sm.api.actor.client.SequenceManagerApiFactory
import esw.sm.api.protocol.ConfigureResponse.Failure
import esw.sm.api.protocol._
import org.HdrHistogram.Histogram
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

object SequenceManagerReliabilityTest extends LocationUtils {

  override def locationService: LocationService                         = HttpLocationServiceFactory.makeLocalClient
  override implicit def actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "perf-test")
  override implicit def patienceConfig: PatienceConfig                  = PatienceConfig(500.seconds, 50.millis)

  LocationServerStatus.requireUpLocally()

  private val sequenceManagerPrefix = Prefix(ESW, "sequence_manager")
  private val smLocation            = resolveAkkaLocation(sequenceManagerPrefix, Service)
  private val smClient              = SequenceManagerApiFactory.makeAkkaClient(smLocation)

  def main(args: Array[String]): Unit = {
    val provisionResponse = smClient.provision(provisionConfigReliability).futureValue
    provisionResponse shouldBe a[ProvisionResponse.Success.type]

    warmUp()
    actualPerf()

    actorSystem.terminate()
  }

  private def warmUp(): Unit = {
    val warmUpConfigureHistogram   = new Histogram(3)
    val warmUpShutdownHistogram    = new Histogram(3)
    val warmUpRestartHistogram     = new Histogram(3)
    val warmUpShutdownSeqHistogram = new Histogram(3)
    repeatScenario(
      warmupIterations,
      "Warmup",
      warmUpConfigureHistogram,
      warmUpShutdownHistogram,
      warmUpRestartHistogram,
      warmUpShutdownSeqHistogram
    )
  }

  private def actualPerf(): Unit = {
    val configureHistogram   = new Histogram(3)
    val shutdownHistogram    = new Histogram(3)
    val restartHistogram     = new Histogram(3)
    val shutdownSeqHistogram = new Histogram(3)
    repeatScenario(actualIterations, "Actual", configureHistogram, shutdownHistogram, restartHistogram, shutdownSeqHistogram)
    recordResults(configureHistogram, "configure_results.txt")
    recordResults(shutdownHistogram, "shutdown_results.txt")
    recordResults(restartHistogram, "restart_results.txt")
    recordResults(shutdownSeqHistogram, "shutdown_seq_results.txt")
  }

  private def repeatScenario(
      times: Int,
      label: String,
      configureHist: Histogram,
      shutdownHist: Histogram,
      restartHist: Histogram,
      shutdownSeqHist: Histogram
  ): Unit = {
    (1 to times).foreach { iterationNumber =>
      println(s"$label iteration ------> $iterationNumber")
      scenario(configureHist, shutdownHist, restartHist, shutdownSeqHist)
    }
    printResults(configureHist)
    printResults(shutdownHist)
    printResults(restartHist)
    printResults(shutdownHist)
  }

  private def scenario(
      configureHist: Histogram,
      shutdownHist: Histogram,
      restartHist: Histogram,
      shutdownSeqHist: Histogram
  ): Unit = {

    // configure obsMode1
    configureObsMode(obsMode1, configureHist)

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // shutdown obsMode1
    shutdownObsMode(obsMode1, shutdownHist)
    Thread.sleep(Constants.timeout)
    // configure obsMode2
    configureObsMode(obsMode2, configureHist)

    // configure obsMode4 ... (non-conflicting with obsMode2)
    configureObsMode(obsMode4, configureHist)

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // restarting all obsMode2 sequencers
    restartSequencers(obsMode2, restartHist)

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // get obsMode details
    getObsModesDetails()

    // shutdown all obsMode2 sequencers individually
    shutdownSequencers(obsMode2, shutdownSeqHist)

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // configure obsMode3 (having conflicting resources with obsMode4)
    configureObsMode(obsMode3, configureHist)

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // shutdown obsMode4 using subsystem shutdown
    getObsModesDetails()
      .filter(_.obsMode == obsMode4)
      .foreach(_.sequencers.subsystems.foreach(shutdownSubsystemSequencers))

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // configure obsMode1
    configureObsMode(obsMode1, configureHist)

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // switch between obsModes1 and obsMode3
    shutdownObsMode(obsMode1, shutdownHist)
    Thread.sleep(Constants.timeout)
    configureObsMode(obsMode3, configureHist)

    // shutdownAll obsModes
    smClient.shutdownAllSequencers().futureValue

  }

  private def shutdownObsMode(obsMode: ObsMode, histogram: Histogram) = {
    val (shutdownResponse, shutdownLatency) = Timing.measureTimeMillis(smClient.shutdownObsModeSequencers(obsMode).futureValue)
    shutdownResponse match {
      case ShutdownSequencersResponse.Success =>
        println(s"Shutdown $obsMode Response --> ShutdownSequencersResponse.Success $obsMode")
      case failure: ShutdownSequencersResponse.Failure => println(Console.RED + s"${failure.getMessage}")
    }
    histogram.recordValue(shutdownLatency)
    shutdownResponse
  }

  private def configureObsMode(obsMode: ObsMode, histogram: Histogram) = {
    val (configureResponse, configureLatency) = Timing.measureTimeMillis(smClient.configure(obsMode).futureValue)
    configureResponse match {
      case ConfigureResponse.Success(masterSequencerComponentId) =>
        println(s"Configure $obsMode Response --> ConfigureResponse.Success $masterSequencerComponentId")
      case failure: Failure => println(Console.RED + s"${failure.getMessage}")
    }
    histogram.recordValue(configureLatency)
    configureResponse
  }

  private def restartSequencers(obsMode: ObsMode, histogram: Histogram): Unit = {
    getObsModesDetails()
      .filter(_.obsMode == obsMode)
      .foreach(obsModeDetails =>
        obsModeDetails.sequencers.subsystems.foreach((subSystem: Subsystem) => restartSequencer(subSystem, obsMode, histogram))
      )
  }

  private def restartSequencer(subSystem: Subsystem, obsMode: ObsMode, histogram: Histogram) = {
    val (restartResponse, restartLatency) = Timing.measureTimeMillis(smClient.restartSequencer(subSystem, obsMode).futureValue)
    restartResponse match {
      case RestartSequencerResponse.Success(componentId) =>
        println(s"Restart $subSystem sequencer response ---> RestartSequencerResponse.Success($componentId)")
      case failure: RestartSequencerResponse.Failure => println(Console.RED + s"${failure.getMessage}")
    }
    histogram.recordValue(restartLatency)
    restartResponse
  }

  private def getObsModesDetails() = {
    val obsModeDetailsResponse = smClient.getObsModesDetails.futureValue
    obsModeDetailsResponse match {
      case ObsModesDetailsResponse.Success(obsModeDetails) => obsModeDetails
      case failure: ObsModesDetailsResponse.Failure => {
        println(Console.RED + s"${failure.getMessage}")
        Nil
      }
    }
  }

  private def shutdownSequencers(obsMode: ObsMode, histogram: Histogram): Unit = {
    getObsModesDetails()
      .filter(_.obsMode == obsMode)
      .foreach(obsModeDetails => {
        obsModeDetails.sequencers.subsystems.foreach((subsystem: Subsystem) => shutdownSequencer(subsystem, obsMode, histogram))
      })
  }

  private def shutdownSequencer(subsystem: Subsystem, obsMode: ObsMode, histogram: Histogram) = {
    val (shutdownResponse, shutdownSeqLatency) =
      Timing.measureTimeMillis(smClient.shutdownSequencer(subsystem, obsMode).futureValue)
    shutdownResponse match {
      case ShutdownSequencersResponse.Success          => println(s"$subsystem sequencer for $obsMode shutdown Successfully")
      case failure: ShutdownSequencersResponse.Failure => println(Console.RED + s"${failure.getMessage}")
    }
    histogram.recordValue(shutdownSeqLatency)
    shutdownResponse
  }

  private def shutdownSubsystemSequencers(subsystem: Subsystem): Unit = {
    val shutdownResponse = smClient.shutdownSubsystemSequencers(subsystem).futureValue
    shutdownResponse match {
      case ShutdownSequencersResponse.Success          => println(s"sequencers for $subsystem shutdown Successfully")
      case failure: ShutdownSequencersResponse.Failure => println(Console.RED + s"${failure.getMessage}")
    }
  }

}
