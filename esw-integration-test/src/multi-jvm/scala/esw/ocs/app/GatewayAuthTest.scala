package esw.ocs.app

import java.nio.file.{Paths, Path => NIOPATH}

import akka.actor.CoordinatedShutdown.UnknownReason
import csw.aas.core.commons.AASConnection
import csw.command.api.scaladsl.CommandService
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.{ComponentId, HttpRegistration}
import csw.network.utils.{Networks, SocketUtils}
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

  private val gatewayUser                   = "gateway-user"
  private val gatewayUserPassword           = "gateway-user"
  private val serverTimeout: FiniteDuration = 3.minutes
  private val keycloakPort                  = SocketUtils.getFreePort
  private val tokenFactory: () => Option[String] =
    () =>
      Some(
        BearerToken
          .fromServer(
            host = Networks().hostname,
            port = keycloakPort,
            username = gatewayUser,
            password = gatewayUserPassword,
            realm = "TMT-test",
            client = "esw-gateway-client"
          )
          .token
      )
  private var keycloakStopHandle: StopHandle = _

  private val mockResolver: Resolver         = mock[Resolver]
  private val commandService: CommandService = mock[CommandService]
  private val prefix                         = Prefix("IRIS.filter.wheel")
  private val componentId                    = ComponentId(prefix, Assembly)
  private val runId                          = Id("1234")
  private val startExposureCommand           = Setup(prefix, CommandName("startExposure"), Some(ObsId("obsId")))
  private var gatewayServerWiring: GatewayWiring   = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    keycloakStopHandle = startKeycloak()
    gatewayServerWiring = startGateway()
    when(mockResolver.commandService(componentId)).thenReturn(Future.successful(commandService))
    when(commandService.submit(startExposureCommand)).thenReturn(Future.successful(Started(runId)))
  }

  override def afterAll(): Unit = {
    keycloakStopHandle.stop()
    gatewayServerWiring.httpService.shutdown(UnknownReason).futureValue
    super.afterAll()
  }
  "Gateway" must {
    "return 401 response for protected route without token | ESW-95" in {
      val clientFactory  = new ClientFactory(gatewayPostClient, gatewayWsClient)
      val commandService = clientFactory.component(componentId)

      val httpError = intercept[HttpError](Await.result(commandService.submit(startExposureCommand), defaultTimeout))
      httpError.statusCode shouldBe 401
    }
    "return 200 response for protected route with token with required role | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClientWithToken(tokenFactory)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val commandService            = clientFactory.component(componentId)

      val submitResponse = Await.result(commandService.submit(startExposureCommand), 10.minutes)
      submitResponse shouldBe a[Started]
    }

  }

  private def startKeycloak(): StopHandle = {
    val irisUserRole = "IRIS-user"

    val `esw-gateway-server` = Client(
      "esw-gateway-server",
      "bearer-only"
    )

    val `esw-gateway-client` =
      Client("esw-gateway-client", "public", passwordGrantEnabled = true, authorizationEnabled = false)

    val keycloakData = KeycloakData(
      AdminUser("admin", "admin"),
      realms = Set(
        Realm(
          "TMT-test",
          clients = Set(`esw-gateway-server`, `esw-gateway-client`),
          users = Set(
            ApplicationUser(
              gatewayUser,
              gatewayUserPassword,
              realmRoles = Set(irisUserRole)
            )
          ),
          realmRoles = Set(irisUserRole)
        )
      )
    )
    val embeddedKeycloak =
      new EmbeddedKeycloak(keycloakData, KeycloakSettings(port = keycloakPort, printProcessLogs = false))
    val stopHandle = Await.result(embeddedKeycloak.startServer(), serverTimeout)
    Await.result(locationService.register(HttpRegistration(AASConnection.value, keycloakPort, "auth")), defaultTimeout)
    stopHandle
  }

  private def startGateway(): GatewayWiring = {
    val commandRolesPath = Paths.get(getClass.getResource("/commandRoles.conf").getPath)
    val gatewayWiring =
      TestGatewayWiring.make(Some(SocketUtils.getFreePort), local = true, commandRolesPath, mockResolver)
    Await.result(gatewayWiring.httpService.registeredLazyBinding, defaultTimeout)
    gatewayWiring
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
