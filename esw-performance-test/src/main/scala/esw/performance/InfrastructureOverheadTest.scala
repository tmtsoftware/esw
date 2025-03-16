package esw.performance

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.client.utils.LocationServerStatus
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandResponse.{SubmitResponse, isNegative}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.gateway.api.clients.ClientFactory
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.testkit.utils.{GatewayUtils, KeycloakUtils}
import esw.performance.constants.InfraOverheadConstants
import esw.performance.utils.PerfUtils.{printResults, recordResults}
import esw.performance.utils.Timing
import org.HdrHistogram.Histogram
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

object InfrastructureOverheadTest extends GatewayUtils with KeycloakUtils {

  override def locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  override implicit def actorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "perf-overhead-test")
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(120.seconds, 50.millis)

  override lazy val keycloakPort: Int = 8081

  LocationServerStatus.requireUpLocally()

  private val loggerFactory = new LoggerFactory(Prefix(ESW, "perfInfraOverhead.test"))
  val log: Logger           = loggerFactory.getLogger

  private def testScenario(eswSequencerClient: SequencerApi, histogram: Histogram): Unit = {
    val sequence = Sequence(Setup(Prefix("ESW.perf.test"), CommandName("command-1"), None))

    val (submitResponse, latency) = Timing.measureTimeMillis(eswSequencerClient.submitAndWait(sequence).futureValue)

    printSubmitResponse(submitResponse)
    histogram.recordValue(latency)
    println(s"Latency Overhead: $latency")
  }

  private def scenarioRepetition(eswSequencerClient: SequencerApi, resultsFile: String): Unit = {
    val histogram       = new Histogram(3)
    val warmUpHistogram = new Histogram(3)

    (1 to InfraOverheadConstants.warmupIterations).foreach { iterationNumber =>
      println(s"Warmup iteration ------> $iterationNumber")
      testScenario(eswSequencerClient, warmUpHistogram)
    }

    (1 to InfraOverheadConstants.actualIterations).foreach { iterationNumber =>
      println(s"Actual iteration ------> $iterationNumber")
      testScenario(eswSequencerClient, histogram)
    }

    recordResults(histogram, resultsFile)
    log.info("Actual Latencies")
    printResults(histogram)
  }

  private def perfTestJvmOnlyScenario(): Unit = {
    // taking location of esw-sequencer
    val eswSequencerLocation = resolvePekkoLocation(Prefix(ESW, "perfTest"), Sequencer)
    val eswSequencerClient   = SequencerApiFactory.make(eswSequencerLocation)
    val resultsFile          = "results_scenario_jvm_only.txt"

    scenarioRepetition(eswSequencerClient, resultsFile)
  }

  private def perfTestWithGatewayScenario(): Unit = {
    val tokenWithEswUserRole: () => Option[String] = getToken("esw-user", "esw-user")
    val gatewayPostClientWithAuth                  = gatewayHTTPClient(tokenWithEswUserRole)

    val clientFactory      = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
    val eswSequencerClient = clientFactory.sequencer(ComponentId(Prefix(ESW, "perfTest"), Sequencer))
    val resultsFile        = "results_scenario_with_gateway.txt"

    scenarioRepetition(eswSequencerClient, resultsFile)
  }

  private def perfTestWithEmbeddedHttpScenario(): Unit = {
    val eswSequencerHttpLocation = resolveHTTPLocation(Prefix(ESW, "perfTest"), Sequencer)
    val eswSequencerClient       = SequencerApiFactory.make(eswSequencerHttpLocation)
    val resultsFile              = "results_scenario_with_http_client.txt"
    scenarioRepetition(eswSequencerClient, resultsFile)
  }

  private def printSubmitResponse(submitResponse: SubmitResponse): Unit = {
    if (isNegative(submitResponse)) println(Console.RED + s"Failed Submit response: $submitResponse")
    else println(Console.BLUE + s"Success Submit response: $submitResponse")
  }

  def main(args: Array[String]): Unit = {
    perfTestJvmOnlyScenario()
    perfTestWithGatewayScenario()
    perfTestWithEmbeddedHttpScenario()
  }
}
