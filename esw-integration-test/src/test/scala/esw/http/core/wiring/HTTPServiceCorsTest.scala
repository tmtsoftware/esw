package esw.http.core.wiring

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.model.headers.{HttpOrigin, Origin}
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route
import csw.location.api.models.{ComponentType, NetworkType}
import csw.location.client.ActorSystemFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.network.utils.Networks
import esw.ocs.testkit.EswTestKit

import scala.concurrent.duration.DurationInt

class HTTPServiceCorsTest extends EswTestKit {

  lazy val route: Route = {
    pathPrefix("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to pekko-http</h1>"))
      }
    }
  }
  private val hostname                                 = Networks(NetworkType.Outside.envKey).hostname
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(10.seconds, 100.millis)

  override lazy val actorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "http-core-server-system")

  // todo ask about overriding the actor system
//  lazy val wiring: ServerWiring = ServerWiring.make(Some(gatewayPort))
//  import wiring.*

  final lazy val actorRuntime = new ActorRuntime(actorSystem)
  import actorRuntime.typedSystem

  private lazy val config = actorSystem.settings.config
  lazy val settings       = new Settings(Some(gatewayPort), None, config, ComponentType.Service)

  private lazy val loggerFactory = new LoggerFactory(settings.httpConnection.prefix)
  lazy val logger: Logger        = loggerFactory.getLogger

  lazy val httpService = new HttpService(logger, locationService, route, settings, actorRuntime)

  lazy val requestOriginHeader = Origin(HttpOrigin(s"http://${hostname}:6000"))

  override def beforeAll(): Unit = {
    super.beforeAll()
    httpService.startAndRegisterServer()
  }

  "Gateway HTTP Service" must {
    "Set CORS Headers when requests from different Origin and  HTTP 200 status response is returned" in {

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              method = HttpMethods.GET,
              uri = Uri(s"http://${hostname}:${gatewayPort}/hello"),
              headers = List(requestOriginHeader)
            )
          )
          .futureValue
      response.status shouldBe StatusCodes.OK
      response.getHeader("Access-Control-Allow-Origin").get().value() shouldBe s"http://${hostname}:6000"
      response.getHeader("Access-Control-Allow-Credentials").get().value() shouldBe "true"
    }
    "Set CORS Headers when requests from different Origin and HTTP 400 range status response is returned" in {

      val response =
        Http()
          .singleRequest(
            HttpRequest(
              method = HttpMethods.GET,
              uri = Uri(s"http://${hostname}:${gatewayPort}/invalidPath"),
              headers = List(requestOriginHeader)
            )
          )
          .futureValue

      response.status shouldBe StatusCodes.NotFound
      response.getHeader("Access-Control-Allow-Origin").get().value() shouldBe s"http://${hostname}:6000"
      response.getHeader("Access-Control-Allow-Credentials").get().value() shouldBe "true"
    }
  }
}
