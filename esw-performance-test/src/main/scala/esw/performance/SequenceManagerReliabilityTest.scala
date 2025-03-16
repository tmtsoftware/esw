package esw.performance

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType.Service
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.client.utils.LocationServerStatus
import csw.logging.client.scaladsl.{LoggerFactory, LoggingSystemFactory}
import csw.network.utils.Networks
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.{ObsMode, VariationInfo}
import esw.ocs.testkit.utils.LocationUtils
import esw.performance.constants.ObsModes.*
import esw.performance.constants.SMReliabilityConstants
import esw.performance.utils.PerfUtils.{printResults, recordResults}
import esw.performance.utils.Timing
import esw.sm.api.actor.client.SequenceManagerApiFactory
import esw.sm.api.protocol.*
import esw.sm.api.protocol.ConfigureResponse.Failure
import org.HdrHistogram.Histogram
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

import scala.util.Try

object SequenceManagerReliabilityTest extends LocationUtils {

  override def locationService: LocationService                         = HttpLocationServiceFactory.makeLocalClient
  override implicit def actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "perf-test")
  override implicit def patienceConfig: PatienceConfig                  = PatienceConfig(500.seconds, 50.millis)

  LocationServerStatus.requireUpLocally()

  private val sequenceManagerPrefix = Prefix(ESW, "sequence_manager")
  private val smLocation            = resolvePekkoLocation(sequenceManagerPrefix, Service)
  LoggingSystemFactory.start("SMReliabilityTest", "0.1.0-SNAPSHOT", Networks().hostname, actorSystem)

  private val smClient      = SequenceManagerApiFactory.makePekkoClient(smLocation)
  private val loggerFactory = new LoggerFactory(Prefix(ESW, "reliability.perf.test"))
  private val log           = loggerFactory.getLogger

  log.info(s"${actorSystem.name} is at address: ${actorSystem.address.toString}")

  def main(args: Array[String]): Unit = {
    val provisionResponse = smClient.provision(SMReliabilityConstants.provisionConfig).futureValue
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
      SMReliabilityConstants.warmupIterations,
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
    repeatScenario(
      SMReliabilityConstants.actualIterations,
      "Actual",
      configureHistogram,
      shutdownHistogram,
      restartHistogram,
      shutdownSeqHistogram
    )
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
      log.info(s"$label iteration ------> $iterationNumber")
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

    // step1: configure obsMode1
    configureObsMode(obsMode1, configureHist)

    // step2: shutdown obsMode1
    shutdownObsMode(obsMode1, shutdownHist)

    // step3: configure obsMode2
    configureObsMode(obsMode2, configureHist)

    // step4: configure obsMode4 ... (non-conflicting with obsMode2)
    configureObsMode(obsMode4, configureHist)

    // step5: restarting all obsMode2 sequencers
    restartSequencers(obsMode2, restartHist)

    // step6: get obsMode details
    getObsModesDetails
    log.info("Fetched obsModes details")
    Thread.sleep(SMReliabilityConstants.timeout)

    // step7: shutdown all obsMode2 sequencers individually
    shutdownSequencers(obsMode2, shutdownSeqHist)

    // step8: configure obsMode3 (having conflicting resources with obsMode4)
    Try {
      configureObsMode(obsMode3, configureHist)
    }.recover(e => log.info(s"Expected Error Scenario: Configure $obsMode3 failed due to: " + e.getMessage))

    // step9: shutdown obsMode4 using subsystem shutdown
    getObsModesDetails
      .filter(_.obsMode == obsMode4)
      .foreach(_.sequencers.variationInfos.foreach(shutdownSubsystemSequencers))

    // step10: configure obsMode1
    configureObsMode(obsMode1, configureHist)

    // step12: shutdown obsMode1 sequencers
    shutdownSequencers(obsMode1, shutdownHist)

    // step11: configure obsMode3 non-conflicting with obsMode1
    configureObsMode(obsMode3, configureHist)

    // step13: shutdownAll obsModes
    shutdownObsMode(obsMode3, shutdownSeqHist)

  }

  private def shutdownObsMode(obsMode: ObsMode, histogram: Histogram): Unit = {
    val (shutdownResponse, shutdownLatency) = Timing.measureTimeMillis(smClient.shutdownObsModeSequencers(obsMode).futureValue)
    shutdownResponse match {
      case ShutdownSequencersResponse.Success =>
        log.info(s"Shutdown $obsMode Response --> ShutdownSequencersResponse.Success $obsMode")
      case failure: ShutdownSequencersResponse.Failure =>
        throw new Exception(s"Failure to shutdownObsMode $obsMode : ${failure.getMessage}")
    }
    histogram.recordValue(shutdownLatency)
    // to simulate actual observation
    Thread.sleep(SMReliabilityConstants.timeout)
  }

  private def configureObsMode(obsMode: ObsMode, histogram: Histogram): Unit = {
    val (configureResponse, configureLatency) = Timing.measureTimeMillis(smClient.configure(obsMode).futureValue)
    configureResponse match {
      case ConfigureResponse.Success(masterSequencerComponentId) =>
        log.info(s"Configure $obsMode Response --> ConfigureResponse.Success $masterSequencerComponentId")
      case failure: Failure =>
        throw new Exception(s"Failure to configure $obsMode : ${failure.getMessage}")
    }
    histogram.recordValue(configureLatency)
    // to simulate actual observation
    Thread.sleep(SMReliabilityConstants.timeout)
  }

  private def restartSequencers(obsMode: ObsMode, histogram: Histogram): Unit = {
    getObsModesDetails
      .find(_.obsMode == obsMode)
      .foreach(obsModeDetails =>
        obsModeDetails.sequencers.variationInfos.foreach((variationInfo: VariationInfo) => {
          restartSequencer(variationInfo.prefix(obsMode), histogram)
        })
      )
  }

  private def restartSequencer(
      prefix: Prefix,
      histogram: Histogram
  ): Unit = {
    val (restartResponse, restartLatency) =
      Timing.measureTimeMillis(smClient.restartSequencer(prefix.subsystem, ObsMode(prefix.componentName)).futureValue)
    restartResponse match {
      case RestartSequencerResponse.Success(componentId) =>
        log.info(s"Restart $prefix sequencer response ---> RestartSequencerResponse.Success($componentId)")
      case failure: RestartSequencerResponse.Failure =>
        throw new Exception(
          s"Failure to restart sequencer of prefix:$prefix : ${failure.getMessage}"
        )
    }
    histogram.recordValue(restartLatency)
    // to simulate actual observation
    Thread.sleep(SMReliabilityConstants.timeout)
  }

  private def getObsModesDetails = {
    val obsModeDetailsResponse = smClient.getObsModesDetails.futureValue
    obsModeDetailsResponse match {
      case ObsModesDetailsResponse.Success(obsModeDetails) => obsModeDetails
      case failure: ObsModesDetailsResponse.Failure =>
        throw new Exception(s"Get obsMode failed : ${failure.getMessage}")
    }
  }

  private def shutdownSequencers(obsMode: ObsMode, histogram: Histogram): Unit = {
    getObsModesDetails
      .find(_.obsMode == obsMode)
      .foreach(obsModeDetails => {
        obsModeDetails.sequencers.variationInfos.foreach((variationInfo: VariationInfo) => {
          shutdownSequencer(variationInfo.prefix(obsMode), histogram)
        })
      })
  }

  private def shutdownSequencer(
      prefix: Prefix,
      histogram: Histogram
  ): Unit = {
    val (shutdownResponse, shutdownSeqLatency) =
      Timing.measureTimeMillis(smClient.shutdownSequencer(prefix.subsystem, ObsMode(prefix.componentName)).futureValue)

    shutdownResponse match {
      case ShutdownSequencersResponse.Success =>
        log.info(s"$prefix sequencer shutdown Successfully")
      case failure: ShutdownSequencersResponse.Failure =>
        throw new Exception(s"Failure to shutdown Sequencer for $prefix : ${failure.getMessage}")
    }
    histogram.recordValue(shutdownSeqLatency)
    // to simulate actual observation
    Thread.sleep(SMReliabilityConstants.timeout)
  }

  private def shutdownSubsystemSequencers(variationInfo: VariationInfo): Unit = {
    val shutdownResponse = smClient.shutdownSubsystemSequencers(variationInfo.subsystem).futureValue
    shutdownResponse match {
      case ShutdownSequencersResponse.Success =>
        log.info(s"sequencers for ${variationInfo.subsystem} shutdown Successfully")
      case failure: ShutdownSequencersResponse.Failure =>
        throw new Exception(s"Failed to shutdown sequencers for ${variationInfo.subsystem} : ${failure.getMessage}")
    }
    // to simulate actual observation
    Thread.sleep(SMReliabilityConstants.timeout)
  }

}
