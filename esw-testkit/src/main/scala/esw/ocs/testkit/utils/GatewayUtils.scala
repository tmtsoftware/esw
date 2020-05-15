package esw.ocs.testkit.utils

import java.nio.file
import java.nio.file.Paths

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import com.typesafe.config.ConfigFactory
import csw.aas.http.SecurityDirectives
import csw.location.api.models.{ComponentType, HttpLocation}
import csw.logging.client.appenders.LogAppenderBuilder
import csw.logging.client.internal.LoggingSystem
import csw.network.utils.SocketUtils
import csw.prefix.models.Prefix
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{PostRequest, WebsocketRequest}
import esw.gateway.server.GatewayWiring
import msocket.api.ContentType
import msocket.impl.post.HttpPostTransport
import msocket.impl.ws.WebsocketTransport

trait GatewayUtils extends LocationUtils with BaseTestSuite with GatewayCodecs {

  private lazy val commandRolesPath = Paths.get(getClass.getResource("/commandRoles.conf").getPath)
  println(s"getClass ==============$getClass")
  private lazy val directives                       = SecurityDirectives.authDisabled(actorSystem.settings.config)
  private var gatewayBinding: Option[ServerBinding] = None
  private var gatewayLocation: Option[HttpLocation] = None

  private var loggingSystem: LoggingSystem = null
  private var gatewayWiring: GatewayWiring = null

  // ESW-98
  private lazy val gatewayPrefix = Prefix(
    ConfigFactory
      .load()
      .getConfig("http-server")
      .getString("prefix")
  )

  lazy val gatewayPort: Int                                      = SocketUtils.getFreePort
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

  def spawnGateway(path: file.Path = commandRolesPath): HttpLocation = {
    gatewayWiring = GatewayWiring.make(Some(gatewayPort), local = true, path, actorSystem, directives)
    val (binding, registration) = gatewayWiring.httpService.registeredLazyBinding.futureValue
    gatewayBinding = Some(binding)
    gatewayLocation = Some(registration.location.asInstanceOf[HttpLocation])
    gatewayLocation.get
  }

  def spawnGatewayWithLogging(_appenderBuilders: List[LogAppenderBuilder]): HttpLocation = {
    val gatewayL = spawnGateway()
    loggingSystem = gatewayWiring.wiring.cswWiring.actorRuntime.startLogging("test", "0.0.1")
    loggingSystem.setAppenders(_appenderBuilders)
    gatewayL
  }

  def shutdownGateway(): Unit = {
    if (gatewayBinding.nonEmpty) {
      gatewayWiring.httpService.shutdown(UnknownReason).futureValue
      if (!loggingSystem.eq(null)) loggingSystem.stop
    }
  }

}
