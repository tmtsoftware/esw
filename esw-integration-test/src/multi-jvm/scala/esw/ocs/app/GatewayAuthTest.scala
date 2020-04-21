package esw.ocs.app

import java.nio.file.{Paths, Path => NIOPATH}

import csw.aas.core.commons.AASConnection
import csw.command.api.scaladsl.CommandService
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.{ComponentId, HttpRegistration}
import csw.network.utils.SocketUtils
import csw.params.commands.CommandResponse.Started
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.{Id, ObsId}
import csw.prefix.models.Prefix
import esw.gateway.api.clients.ClientFactory
import esw.gateway.server.GatewayWiring
import esw.gateway.server.utils.Resolver
import esw.ocs.testkit.EswTestKit
import msocket.impl.HttpError
import org.tmt.embedded_keycloak.KeycloakData._
import org.tmt.embedded_keycloak.impl.StopHandle
import org.tmt.embedded_keycloak.utils.BearerToken
import org.tmt.embedded_keycloak.{EmbeddedKeycloak, KeycloakData, Settings => KeycloakSettings}

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Await, Future}

class GatewayAuthTest extends EswTestKit {

  private val gatewayUser         = "gateway-user"
  private val gatewayUserPassword = "gateway-user"

  private val timeout: FiniteDuration = 20.seconds
  private val serverTimeout: FiniteDuration  = 5.minutes

  private val keycloakPort = SocketUtils.getFreePort

  private val tokenFactory: () => Option[String] =
    () =>
      Some(
        BearerToken
          .fromServer(
            host = "localhost",
            port = keycloakPort,
            username = gatewayUser,
            password = gatewayUserPassword,
            realm = "TMT",
            client = "esw-gateway-client"
          )
          .token
      )

  val mockResolver: Resolver         = mock[Resolver]
  val commandService: CommandService = mock[CommandService]
  val prefix                         = Prefix("TCS.test")
  val componentId                    = ComponentId(prefix, Assembly)
  val runId                          = Id("1234")
  val longRunningCommand             = Setup(prefix, CommandName("long-running"), Some(ObsId("obsId")))

  private var keycloakStopHandle: StopHandle = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    keycloakStopHandle = startKeycloak()
    startGateway()
    when(mockResolver.commandService(componentId)).thenReturn(Future.successful(commandService))
    when(commandService.submit(longRunningCommand)).thenReturn(Future.successful(Started(runId)))
  }

  override def afterAll(): Unit = {
    keycloakStopHandle.stop()
    super.afterAll()
  }
  "Gateway" must {
    "return 401 response for protected route without token" in {
      val clientFactory      = new ClientFactory(gatewayPostClient, gatewayWsClient)
      val commandService     = clientFactory.component(componentId)
      val longRunningCommand = Setup(prefix, CommandName("long-running"), Some(ObsId("obsId")))

      val httpError = intercept[HttpError](Await.result(commandService.submit(longRunningCommand), timeout))
      httpError.statusCode shouldBe 401
    }
  }

  private def startKeycloak(): StopHandle = {
    val tcsUserRole = "TCS-user"

    val `esw-gateway-server` = Client(
      "esw-gateway-server",
      "bearer-only"
    )

    val `esw-gateway-client` =
      Client("esw-gateway-client", "public", passwordGrantEnabled = true, authorizationEnabled = false)

    val keycloakData = KeycloakData(
      realms = Set(
        Realm(
          "TMT",
          clients = Set(`esw-gateway-server`, `esw-gateway-client`),
          users = Set(
            ApplicationUser(
              gatewayUser,
              gatewayUserPassword,
              realmRoles = Set(tcsUserRole)
            )
          ),
          realmRoles = Set(tcsUserRole)
        )
      )
    )
    val embeddedKeycloak =
      new EmbeddedKeycloak(keycloakData, KeycloakSettings(port = keycloakPort, printProcessLogs = false))
    val stopHandle = Await.result(embeddedKeycloak.startServer(), serverTimeout)
    Await.result(locationService.register(HttpRegistration(AASConnection.value, keycloakPort, "auth")), timeout)
    stopHandle
  }

  private def startGateway() = {
    val commandRolesPath = Paths.get(getClass.getResource("/commandRoles.conf").getPath)
    val serverWiring =
      TestGatewayWiring.make(Some(SocketUtils.getFreePort), local = true, commandRolesPath, mockResolver)
    Await.result(serverWiring.httpService.registeredLazyBinding, serverTimeout)
  }

}

object TestGatewayWiring {
  private[esw] def make(
      _port: Option[Int],
      local: Boolean,
      commandRoleConfigPath: NIOPATH,
      _resolver: Resolver
  ): GatewayWiring =
    new GatewayWiring(_port, local, commandRoleConfigPath) {
      override val resolver: Resolver = _resolver
    }
}
