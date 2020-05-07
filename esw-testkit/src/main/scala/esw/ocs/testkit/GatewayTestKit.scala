package esw.ocs.testkit

import java.nio.file.Paths

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import com.typesafe.config.ConfigFactory
import csw.aas.http.SecurityDirectives
import csw.location.api.models.{ComponentType, HttpLocation}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.network.utils.SocketUtils
import csw.prefix.models.Prefix
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{PostRequest, WebsocketRequest}
import esw.gateway.server.GatewayWiring
import esw.ocs.testkit.utils.BaseTestSuite
import msocket.api.ContentType
import msocket.impl.post.HttpPostTransport
import msocket.impl.ws.WebsocketTransport

import scala.concurrent.ExecutionContext

class GatewayTestKit(val locationService: LocationService)(implicit system: ActorSystem[SpawnProtocol.Command])
    extends LocationUtils
    with BaseTestSuite
    with GatewayCodecs {

  implicit val ec: ExecutionContext = system.executionContext
  // gateway
  lazy val gatewayPort: Int                          = SocketUtils.getFreePort
  private val commandRolesPath                       = Paths.get(getClass.getResource("/commandRoles.conf").getPath)
  private val securityDirectives: SecurityDirectives = SecurityDirectives.authDisabled(system.settings.config)
  lazy val gatewayWiring: GatewayWiring =
    GatewayWiring.make(Some(gatewayPort), local = true, commandRolesPath, system, securityDirectives)
  private var gatewayBinding: Option[ServerBinding] = None
  private var gatewayLocation: Option[HttpLocation] = None
  // ESW-98
  private lazy val gatewayPrefix = Prefix(
    ConfigFactory
      .load()
      .getConfig("http-server")
      .getString("prefix")
  )
  lazy val gatewayPostClient: HttpPostTransport[PostRequest]     = gatewayHTTPClient()
  lazy val gatewayWsClient: WebsocketTransport[WebsocketRequest] = gatewayWebSocketClient(gatewayPrefix)

  // ESW-98, ESW-95
  private[esw] def gatewayHTTPClient(tokenFactory: () => Option[String] = () => None) = {
    val httpLocation = resolveHTTPLocation(gatewayPrefix, ComponentType.Service)
    val httpUri      = Uri(httpLocation.uri.toString).withPath(Path("/post-endpoint")).toString()
    val httpClient =
      new HttpPostTransport[PostRequest](httpUri, ContentType.Json, tokenFactory)
    httpClient
  }

  private def gatewayWebSocketClient(prefix: Prefix) = {
    val httpLocation = resolveHTTPLocation(prefix, ComponentType.Service)
    val webSocketUri = Uri(httpLocation.uri.toString).withScheme("ws").withPath(Path("/websocket-endpoint")).toString()
    val webSocketClient =
      new WebsocketTransport[WebsocketRequest](webSocketUri, ContentType.Json)
    webSocketClient
  }

  def spawnGateway(): HttpLocation = {
    val (binding, registration) = gatewayWiring.httpService.registeredLazyBinding.futureValue
    gatewayBinding = Some(binding)
    gatewayLocation = Some(registration.location.asInstanceOf[HttpLocation])
    gatewayLocation.get
  }

  def shutdownGateway(): Unit =
    if (gatewayBinding.nonEmpty)
      gatewayWiring.httpService.shutdown(UnknownReason).futureValue

}

object GatewayTestKit {
  def apply(
      system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "gateway-testkit")
  ): GatewayTestKit =
    new GatewayTestKit(HttpLocationServiceFactory.makeLocalClient(system))(system)
}
