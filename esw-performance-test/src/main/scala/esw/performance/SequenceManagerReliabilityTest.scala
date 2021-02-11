package esw.performance

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType.Service
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.client.utils.LocationServerStatus
import csw.logging.client.scaladsl.LoggerFactory
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
  private val loggerFactory         = new LoggerFactory(Prefix(ESW, "reliability.perf.test"))
  private val log                   = loggerFactory.getLogger

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

    var step = 1
    // step1: configure obsMode1
    println(s"----------> step $step")
    log.info(s"----------> step $step")
    configureObsMode(obsMode1, configureHist)
    step += 1

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // step2: shutdown obsMode1
    println(s"----------> step $step")
    log.info(s"----------> step $step")
    shutdownObsMode(obsMode1, shutdownHist)
    step += 1

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // step3: configure obsMode2
    println(s"----------> step $step")
    log.info(s"----------> step $step")
    configureObsMode(obsMode2, configureHist)
    step += 1

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // step4: configure obsMode4 ... (non-conflicting with obsMode2)
    println(s"----------> step $step")
    log.info(s"----------> step $step")
    configureObsMode(obsMode4, configureHist)
    step += 1

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // step5: restarting all obsMode2 sequencers
    println(s"----------> step $step")
    log.info(s"----------> step $step")
    restartSequencers(obsMode2, restartHist)
    step += 1

    // step6: get obsMode details
    println(s"----------> step $step")
    log.info(s"----------> step $step")
    println("Fetched obsModes details")
    getObsModesDetails()
    step += 1

    // step7: shutdown all obsMode2 sequencers individually
    println(s"----------> step $step")
    log.info(s"----------> step $step")
    shutdownSequencers(obsMode2, shutdownSeqHist)
    step += 1

    // step8: configure obsMode3 (having conflicting resources with obsMode4)
    println(s"----------> step $step")
    log.info(s"----------> step $step")
    configureObsMode(obsMode3, configureHist)
    step += 1

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // step9: shutdown obsMode4 using subsystem shutdown
    println(s"----------> step $step")
    log.info(s"----------> step $step")
    getObsModesDetails()
      .filter(_.obsMode == obsMode4)
      .foreach(_.sequencers.subsystems.foreach(shutdownSubsystemSequencers))
    step += 1

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // step10: configure obsMode1
    println(s"----------> step $step")
    log.info(s"----------> step $step")
    configureObsMode(obsMode1, configureHist)
    step += 1

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // step11: configure obsMode3 non-conflicting with obsMode1
    println(s"----------> step $step")
    log.info(s"----------> step $step")
    configureObsMode(obsMode3, configureHist)
    step += 1

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // step12: shutdown obsMode1 sequencers
    println(s"----------> step $step")
    log.info(s"----------> step $step")
    shutdownSequencers(obsMode1, shutdownHist)
    step += 1

    // step13: shutdownAll obsModes
    println(s"----------> step $step")
    log.info(s"----------> step $step")
    shutdownObsMode(obsMode3, shutdownSeqHist)
    step += 1

    Thread.sleep(Constants.timeout)
  }

  private def shutdownObsMode(obsMode: ObsMode, histogram: Histogram) = {
    val (shutdownResponse, shutdownLatency) = Timing.measureTimeMillis(smClient.shutdownObsModeSequencers(obsMode).futureValue)
    shutdownResponse match {
      case ShutdownSequencersResponse.Success =>
        println(s"Shutdown $obsMode Response --> ShutdownSequencersResponse.Success $obsMode")
        log.info(s"Shutdown $obsMode Response --> ShutdownSequencersResponse.Success $obsMode")
      case failure: ShutdownSequencersResponse.Failure => {
        println(Console.RED + s"${failure.getMessage}")
        log.error(s"Failure to shutdownObsMode $obsMode : ${failure.getMessage}")

      }
    }
    histogram.recordValue(shutdownLatency)
    shutdownResponse
  }

  private def configureObsMode(obsMode: ObsMode, histogram: Histogram) = {
    val (configureResponse, configureLatency) = Timing.measureTimeMillis(smClient.configure(obsMode).futureValue)
    configureResponse match {
      case ConfigureResponse.Success(masterSequencerComponentId) =>
        println(s"Configure $obsMode Response --> ConfigureResponse.Success $masterSequencerComponentId")
        log.info(s"Configure $obsMode Response --> ConfigureResponse.Success $masterSequencerComponentId")
      case failure: Failure => {
        println(Console.RED + s"${failure.getMessage}")
        log.error(s"Failure to configure $obsMode : ${failure.getMessage}")
      }
    }
    histogram.recordValue(configureLatency)
    configureResponse
  }

  private def restartSequencers(obsMode: ObsMode, histogram: Histogram): Unit = {
    getObsModesDetails()
      .filter(_.obsMode == obsMode)
      .foreach(obsModeDetails =>
        obsModeDetails.sequencers.subsystems.foreach((subSystem: Subsystem) => {
          restartSequencer(subSystem, obsMode, histogram)
          // to simulate actual observation
          Thread.sleep(Constants.timeout)
        })
      )
  }

  private def restartSequencer(subSystem: Subsystem, obsMode: ObsMode, histogram: Histogram) = {
    val (restartResponse, restartLatency) = Timing.measureTimeMillis(smClient.restartSequencer(subSystem, obsMode).futureValue)
    restartResponse match {
      case RestartSequencerResponse.Success(componentId) => {
        println(s"Restart $subSystem sequencer response ---> RestartSequencerResponse.Success($componentId)")
        log.info(s"Restart $subSystem sequencer response ---> RestartSequencerResponse.Success($componentId)")
      }
      case failure: RestartSequencerResponse.Failure => {
        println(Console.RED + s"${failure.getMessage}")
        log.error(s"Failure to restart sequencer of subsystem:$subSystem, obsMode:$obsMode : ${failure.getMessage}")
      }
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
        log.error(s"Get obsMode failed : ${failure.getMessage}")
        Nil
      }
    }
  }

  private def shutdownSequencers(obsMode: ObsMode, histogram: Histogram): Unit = {
    getObsModesDetails()
      .filter(_.obsMode == obsMode)
      .foreach(obsModeDetails => {
        obsModeDetails.sequencers.subsystems.foreach((subsystem: Subsystem) => {
          shutdownSequencer(subsystem, obsMode, histogram)
          // to simulate actual observation
          Thread.sleep(Constants.timeout)
        })
      })
  }

  private def shutdownSequencer(subsystem: Subsystem, obsMode: ObsMode, histogram: Histogram) = {
    val (shutdownResponse, shutdownSeqLatency) =
      Timing.measureTimeMillis(smClient.shutdownSequencer(subsystem, obsMode).futureValue)
    shutdownResponse match {
      case ShutdownSequencersResponse.Success => {
        println(s"$subsystem sequencer for $obsMode shutdown Successfully")
        log.info(s"$subsystem sequencer for $obsMode shutdown Successfully")
      }
      case failure: ShutdownSequencersResponse.Failure => {
        println(Console.RED + s"${failure.getMessage}")
        log.error(s"Failure to shutdown Sequencer for $subsystem, $obsMode : ${failure.getMessage}")
      }
    }
    histogram.recordValue(shutdownSeqLatency)
    shutdownResponse
  }

  private def shutdownSubsystemSequencers(subsystem: Subsystem): Unit = {
    val shutdownResponse = smClient.shutdownSubsystemSequencers(subsystem).futureValue
    shutdownResponse match {
      case ShutdownSequencersResponse.Success => {
        println(s"sequencers for $subsystem shutdown Successfully")
        log.info(s"sequencers for $subsystem shutdown Successfully")
      }
      case failure: ShutdownSequencersResponse.Failure => {
        println(Console.RED + s"${failure.getMessage}")
        log.info(s" ${failure.getMessage}")
      }
    }
  }

}
