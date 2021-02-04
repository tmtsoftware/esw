package esw.performance

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.client.utils.LocationServerStatus
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.network.utils.Networks
import csw.params.commands.{CommandName, CommandResponse, Sequence, Setup}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.gateway.api.clients.ClientFactory
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.testkit.utils.{GatewayUtils, KeycloakUtils}
import esw.performance.Constants.{actualIterationsOverhead, warmupIterationsOverhead}
import esw.performance.utils.PerfUtils.{printResults, recordResults}
import org.HdrHistogram.Histogram
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.tmt.embedded_keycloak.utils.BearerToken

object InfrastructureOverheadTest extends GatewayUtils with KeycloakUtils {

  override def locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  override implicit def actorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "perf-overhead-test")
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(120.seconds, 50.millis)

  LocationServerStatus.requireUpLocally()

  val loggerFactory = new LoggerFactory(Prefix(ESW, "perfInfraOverhead.test"))
  val log: Logger   = loggerFactory.getLogger

  def testScenario(eswSequencerClient: SequencerApi, sequence: Sequence, histogram: Histogram) = {
    val beforeTime     = System.currentTimeMillis()
    val submitResponse = eswSequencerClient.submitAndWait(sequence).futureValue
    val afterTime      = System.currentTimeMillis()
    submitResponse match {
      case CommandResponse.Started(runId)           => println(Console.BLUE + s"Submit response: started $runId")
      case CommandResponse.Completed(runId, result) => println(Console.BLUE + s"Submit response: Completed $runId, $result")
      case CommandResponse.Invalid(runId, issue)    => println(Console.RED + s"Submit response: Invalid $runId, $issue")
      case CommandResponse.Error(runId, message)    => println(Console.RED + s"Submit response: Error $runId, $message")
      case CommandResponse.Cancelled(runId)         => println(Console.RED + s"Submit response: Cancelled $runId")
      case CommandResponse.Locked(runId)            => println(Console.RED + s"Submit response: Locked $runId")
    }
    val latency = afterTime - beforeTime
    histogram.recordValue(latency)
    println(s"Latency Overhead: $latency")

  }

  override def getToken(tokenUserName: String, tokenPassword: String, client: String): () => Some[String] = { () =>
    Some(
      BearerToken
        .fromServer(
          host = Networks().hostname,
          port = 8081,
          username = tokenUserName,
          password = tokenPassword,
          realm = "TMT",
          client = client
        )
        .token
    )
  }

  private def scenarioRepetition(
      histogram: Histogram,
      warmUpHistogram: Histogram,
      eswSequencerClient: SequencerApi,
      sequence: Sequence,
      resultsFile: String
  ) = {
    (1 to warmupIterationsOverhead).foreach { iterationNumber =>
      println(s"Warmup iteration ------> $iterationNumber")
      testScenario(eswSequencerClient, sequence, warmUpHistogram)
    }

    (1 to actualIterationsOverhead).foreach { iterationNumber =>
      println(s"Actual iteration ------> $iterationNumber")
      testScenario(eswSequencerClient, sequence, histogram)
    }

    recordResults(histogram, resultsFile)
    log.info("Actual Latencies")
    printResults(histogram)
  }

  def perfTestJvmOnlyScenario(): Unit = {
    val histogram       = new Histogram(3)
    val warmUpHistogram = new Histogram(3)

    // taking location of esw-sequencer
    val eswSequencerLocation = resolveAkkaLocation(Prefix(ESW, "perfTest"), Sequencer)
    val eswSequencerClient   = SequencerApiFactory.make(eswSequencerLocation)
    val sequence             = Sequence(Setup(Prefix("ESW.perf.test"), CommandName("command-1"), None))
    val resultsFile          = "results_scenario_jvm_only.txt"

    scenarioRepetition(histogram, warmUpHistogram, eswSequencerClient, sequence, resultsFile)
  }

  def perfTestWithGatewayScenario(): Unit = {
    val histogram       = new Histogram(3)
    val warmUpHistogram = new Histogram(3)

    lazy val perfRoleEswUserEng                         = "esw-user"
    lazy val perfUser1Password                          = "esw-user"
    lazy val tokenWithEswUserRole: () => Option[String] = getToken(perfRoleEswUserEng, perfUser1Password)
    val gatewayPostClientWithAuth                       = gatewayHTTPClient(tokenWithEswUserRole)

    val clientFactory      = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
    val eswSequencerClient = clientFactory.sequencer(ComponentId(Prefix(ESW, "perfTest"), Sequencer))
    val sequence           = Sequence(Setup(Prefix("ESW.perf.test"), CommandName("command-1"), None))
    val resultsFile        = "results_scenario_with_gateway.txt"

    scenarioRepetition(histogram, warmUpHistogram, eswSequencerClient, sequence, resultsFile)
  }

  def perfTestWithEmbeddedHttpScenario(): Unit = {
    val histogram       = new Histogram(3)
    val warmUpHistogram = new Histogram(3)

    val eswSequencerHttpLocation = resolveHTTPLocation(Prefix(ESW, "perfTest"), Sequencer)
    val eswSequencerClient       = SequencerApiFactory.make(eswSequencerHttpLocation)
    val sequence                 = Sequence(Setup(Prefix("ESW.perf.test"), CommandName("command-1"), None))
    val resultsFile              = "results_scenario_with_http_client.txt"
    scenarioRepetition(histogram, warmUpHistogram, eswSequencerClient, sequence, resultsFile)
  }

  def main(args: Array[String]): Unit = {
    perfTestJvmOnlyScenario()
    perfTestWithGatewayScenario()
    perfTestWithEmbeddedHttpScenario()
  }
}
