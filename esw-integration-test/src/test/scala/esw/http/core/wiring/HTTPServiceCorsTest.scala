package esw.http.core.wiring

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{HttpOrigin, Origin}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.location.api.models.NetworkType
import csw.network.utils.Networks
import esw.ocs.testkit.EswTestKit

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class HTTPServiceCorsTest extends EswTestKit {

  lazy val route: Route = {
    pathPrefix("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    }
  }
  private val hostname                                 = Networks(NetworkType.Public.envKey).hostname
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(10.seconds, 100.millis)

  lazy val wiring = ServerWiring.make(Some(gatewayPort))
  import wiring._
  lazy val httpService = new HttpService(logger, locationService, route, settings, cswWiring.actorRuntime)

  lazy val requestOriginHeader = Origin(HttpOrigin(s"http://${hostname}:6000"))

  override def beforeAll(): Unit = {
    super.beforeAll()
    httpService.registeredLazyBinding
  }

  "Gateway HTTP Service" must {
    "Set CORS Headers when requests from different Origin and  HTTP 200 status response is returned" in {

      val response =
        Await.result(
          Http().singleRequest(
            HttpRequest(
              method = HttpMethods.GET,
              uri = Uri(s"http://${hostname}:${gatewayPort}/hello"),
              headers = List(requestOriginHeader)
            )
          ),
          2.seconds
        )
      response.status shouldBe StatusCodes.OK
      response.getHeader("Access-Control-Allow-Origin").get().value() shouldBe s"http://${hostname}:6000"
      response.getHeader("Access-Control-Allow-Credentials").get().value() shouldBe "true"
    }
    "Set CORS Headers when requests from different Origin and HTTP 400 range status response is returned" in {

      val response =
        Await.result(
          Http().singleRequest(
            HttpRequest(
              method = HttpMethods.GET,
              uri = Uri(s"http://${hostname}:${gatewayPort}/invalidPath"),
              headers = List(requestOriginHeader)
            )
          ),
          2.seconds
        )

      response.status shouldBe StatusCodes.NotFound
      response.getHeader("Access-Control-Allow-Origin").get().value() shouldBe s"http://${hostname}:6000"
      response.getHeader("Access-Control-Allow-Credentials").get().value() shouldBe "true"
    }
  }
}
