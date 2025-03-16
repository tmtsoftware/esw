package esw.ocs.testkit.utils

import java.nio.file
import java.nio.file.Paths

import org.apache.pekko.actor.CoordinatedShutdown.UnknownReason
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.model.Uri.Path
import com.typesafe.config.ConfigFactory
import csw.aas.http.SecurityDirectives
import csw.location.api.models.ComponentType
import csw.network.utils.SocketUtils
import csw.prefix.models.Prefix
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{GatewayRequest, GatewayStreamRequest}
import esw.gateway.server.GatewayWiring
import msocket.api.ContentType
import msocket.http.post.HttpPostTransport
import msocket.http.ws.WebsocketTransport

trait GatewayUtils extends LocationUtils with GatewayCodecs {

  private lazy val commandRolesPath = Paths.get(getClass.getResource("/commandRoles.conf").getPath)
  private lazy val directives       = SecurityDirectives.authDisabled(actorSystem.settings.config)

  private var gatewayWiring: Option[GatewayWiring] = None

  // ESW-98
  private lazy val gatewayPrefix = Prefix(
    ConfigFactory
      .load()
      .getConfig("http-server")
      .getString("prefix")
  )

  lazy val gatewayPort: Int                                          = SocketUtils.getFreePort
  lazy val gatewayPostClient: HttpPostTransport[GatewayRequest]      = gatewayHTTPClient()
  lazy val gatewayWsClient: WebsocketTransport[GatewayStreamRequest] = gatewayWebSocketClient(gatewayPrefix)

  // ESW-98, ESW-95
  private[esw] def gatewayHTTPClient(tokenFactory: () => Option[String] = () => None) = {
    val httpLocation = resolveHTTPLocation(gatewayPrefix, ComponentType.Service)
    val httpUri      = Uri(httpLocation.uri.toString).withPath(Path("/post-endpoint")).toString()
    new HttpPostTransport[GatewayRequest](httpUri, ContentType.Json, tokenFactory)
  }

  private def gatewayWebSocketClient(prefix: Prefix) = {
    val httpLocation = resolveHTTPLocation(prefix, ComponentType.Service)
    val webSocketUri = Uri(httpLocation.uri.toString).withScheme("ws").withPath(Path("/websocket-endpoint")).toString()
    new WebsocketTransport[GatewayStreamRequest](webSocketUri, ContentType.Json)
  }

  def spawnGateway(authEnabled: Boolean = false, path: file.Path = commandRolesPath): GatewayWiring = {
    val wiring =
      if (authEnabled) GatewayWiring.make(Some(gatewayPort), local = true, path, actorSystem)
      else GatewayWiring.make(Some(gatewayPort), local = true, path, actorSystem, directives)

    gatewayWiring = Some(wiring)
    wiring.httpService.startAndRegisterServer().futureValue
    wiring
  }

  def shutdownGateway(): Unit = gatewayWiring.foreach(_.actorRuntime.shutdown(UnknownReason).futureValue)
}
