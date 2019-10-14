package esw.ocs.core

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import csw.admin.server.wiring.AdminWiring
import csw.location.models.AkkaLocation
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.logging.models.Level.{ERROR, FATAL}
import csw.logging.models.LogMetadata
import csw.logging.models.codecs.LoggingCodecs
import csw.network.utils.Networks
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.app.wiring.SequencerWiring
import mscoket.impl.HttpCodecs

import scala.concurrent.duration.DurationLong

class DynamicLogLevelTest extends ScalaTestFrameworkTestKit with BaseTestSuite with LoggingCodecs with HttpCodecs {
  import frameworkTestKit._
  private implicit val sys: ActorSystem[SpawnProtocol.Command] = actorSystem
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  private var wiring: SequencerWiring         = _
  private var adminWiring: AdminWiring        = _
  private val adminPort                       = 7888
  private var sequencerLocation: AkkaLocation = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    LoggingSystemFactory.start("logging", "version", "localhost", sys)
    adminWiring = AdminWiring.make(Some(adminPort))
    adminWiring.adminHttpService.registeredLazyBinding.futureValue
    wiring = new SequencerWiring("esw", "darknight", None)
    sequencerLocation = wiring.sequencerServer.start().rightValue
  }

  override protected def afterAll(): Unit = {
    wiring.sequencerServer.shutDown().futureValue
    super.afterAll()
  }

  "get/set log level for sequencer dynamically | ESW-183" in {
    val defaultLogLevel = FATAL
    val newLogLevel     = ERROR

    val setLogMetadataUri = Uri.from(
      scheme = "http",
      host = Networks().hostname,
      port = adminPort,
      path = s"/admin/logging/${sequencerLocation.connection.name}/level",
      queryString = Some(s"value=$newLogLevel")
    )

    // Get initial log levels
    getLogMetadataAndAssertResponse(LogMetadata(defaultLogLevel, defaultLogLevel, defaultLogLevel, defaultLogLevel))

    // set sequencer log level to FATAL
    val setLogMetadataRequest = HttpRequest(HttpMethods.POST, uri = setLogMetadataUri)
    val setResponse           = Http().singleRequest(setLogMetadataRequest).futureValue
    setResponse.status should ===(StatusCodes.OK)

    // assert sequencer log level is changed to FATAL
    getLogMetadataAndAssertResponse(LogMetadata(defaultLogLevel, defaultLogLevel, defaultLogLevel, newLogLevel))

  }

  private def getLogMetadataAndAssertResponse(expectedLogMetadata: LogMetadata) = {
    val getLogMetadataUri = Uri.from(
      scheme = "http",
      host = Networks().hostname,
      port = adminPort,
      path = s"/admin/logging/${sequencerLocation.connection.name}/level"
    )

    val request  = HttpRequest(HttpMethods.GET, uri = getLogMetadataUri)
    val response = Http().singleRequest(request).futureValue
    response.status should ===(StatusCodes.OK)

    val actualLogMetadata = Unmarshal(response).to[LogMetadata].futureValue
    actualLogMetadata should ===(expectedLogMetadata)
  }

}
