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
    val histogram       = new Histogram(3)
    val warmUpHistogram = new Histogram(3)

    val provisionResponse = smClient.provision(provisionConfig).futureValue
    provisionResponse shouldBe a[ProvisionResponse.Success.type]

    repeatScenario(warmupIterations, warmUpHistogram, "Warmup")
    repeatScenario(actualIterations, histogram, "Actual")
    recordResults(histogram, "results.txt")

    actorSystem.terminate()
  }

  private def repeatScenario(times: Int, histogram: Histogram, label: String): Unit = {
    (1 to times).foreach { iterationNumber =>
      println(s"$label iteration ------> $iterationNumber")
      scenario(histogram)
    }

    //log.info(s"$label latencies")
    printResults(histogram)
  }

  private def scenario(histogram: Histogram): Unit = {
    // configure obsMode1
    configureObsMode(obsMode1)

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // shutdown obsMode1
    shutdownObsMode(obsMode1)
    Thread.sleep(Constants.timeout)
    // configure obsMode2
    configureObsMode(obsMode2)

    // configure obsMode4 ... (non-conflicting with obsMode2)
    configureObsMode(obsMode4)

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // restarting all obsMode2 sequencers
    restartSequencers(obsMode2)

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // get obsMode details
    getObsModesDetails()

    // shutdown all obsMode2 sequencers individually
    shutdownSequencers(obsMode2)

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // configure obsMode3 (having conflicting resources with obsMode4)
    configureObsMode(obsMode3)

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // shutdown obsMode4 using subsystem shutdown
    getObsModesDetails()
      .filter(_.obsMode == obsMode4)
      .foreach(_.sequencers.subsystems.foreach(shutdownSubsystemSequencers))

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // configure obsMode1
    configureObsMode(obsMode1)

    // to simulate actual observation
    Thread.sleep(Constants.timeout)

    // switch between obsModes1 and obsMode3
    shutdownObsMode(obsMode1)
    Thread.sleep(Constants.timeout)
    configureObsMode(obsMode3)

    // shutdownAll obsModes
    smClient.shutdownAllSequencers().futureValue

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

  private def restartSequencers(obsMode: ObsMode): Unit = {
    getObsModesDetails()
      .filter(_.obsMode == obsMode)
      .foreach(obsModeDetails => obsModeDetails.sequencers.subsystems.foreach(restartSequencer(_, obsMode)))
  }

  private def restartSequencer(subSystem: Subsystem, obsMode: ObsMode): Unit = {
    val restartResponse = smClient.restartSequencer(subSystem, obsMode).futureValue
    restartResponse match {
      case RestartSequencerResponse.Success(componentId) =>
        println(s"Restart $subSystem sequencer response ---> RestartSequencerResponse.Success($componentId)")
      case failure: RestartSequencerResponse.Failure => println(Console.RED + s"${failure.getMessage}")
    }
  }

  private def getObsModesDetails() = {
    val obsModeDetailsResponse = smClient.getObsModesDetails.futureValue
    obsModeDetailsResponse match {
      case ObsModesDetailsResponse.Success(obsModeDetails) => {
        println(s"obsmode----------details $obsModeDetails")
        obsModeDetails
      }
      case failure: ObsModesDetailsResponse.Failure => {
        println(Console.RED + s"${failure.getMessage}")
        Nil
      }
    }
  }

  private def shutdownSequencers(obsMode: ObsMode): Unit = {
    getObsModesDetails()
      .filter(_.obsMode == obsMode)
      .foreach(obsModeDetails => {
        obsModeDetails.sequencers.subsystems.foreach(shutdownSequencer(_, obsMode))
      })
  }

  private def shutdownSequencer(subsystem: Subsystem, obsMode: ObsMode): Unit = {
    val shutdownResponse = smClient.shutdownSequencer(subsystem, obsMode).futureValue
    shutdownResponse match {
      case ShutdownSequencersResponse.Success          => println(s"$subsystem sequencer for $obsMode shutdown Successfully")
      case failure: ShutdownSequencersResponse.Failure => println(Console.RED + s"${failure.getMessage}")
    }
  }

  private def shutdownSubsystemSequencers(subsystem: Subsystem): Unit = {
    val shutdownResponse = smClient.shutdownSubsystemSequencers(subsystem).futureValue
    shutdownResponse match {
      case ShutdownSequencersResponse.Success          => println(s"sequencers for $subsystem shutdown Successfully")
      case failure: ShutdownSequencersResponse.Failure => println(Console.RED + s"${failure.getMessage}")
    }
  }

}
