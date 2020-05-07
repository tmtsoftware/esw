package esw.gateway.server

import java.nio.file.Paths

import akka.actor.CoordinatedShutdown.UnknownReason
import csw.aas.core.commons.AASConnection
import csw.command.api.scaladsl.CommandService
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.{ComponentId, ComponentType, HttpRegistration}
import csw.network.utils.{Networks, SocketUtils}
import csw.params.commands.CommandResponse.Started
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, ObsId}
import csw.prefix.models.Prefix
import esw.gateway.api.clients.ClientFactory
import esw.gateway.server.utils.Resolver
import esw.ocs.api.SequencerApi
import esw.ocs.testkit.EswTestKit
import msocket.impl.HttpError
import org.tmt.embedded_keycloak.KeycloakData._
import org.tmt.embedded_keycloak.impl.StopHandle
import org.tmt.embedded_keycloak.utils.BearerToken
import org.tmt.embedded_keycloak.{EmbeddedKeycloak, KeycloakData, Settings => KeycloakSettings}

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Await, Future}

class GatewayAuthTest extends EswTestKit {

  import gatewayTestKit.{gatewayHTTPClient, gatewayWsClient}

  private val gatewayUser1WithRequiredRole    = "gateway-user1"
  private val gatewayUser1Password            = "gateway-user1"
  private val gatewayUser2WithoutRequiredRole = "gateway-user2"
  private val gatewayUser2Password            = "gateway-user2"
  private val serverTimeout: FiniteDuration   = 3.minutes
  private val keycloakPort                    = SocketUtils.getFreePort
  private val validTokenFactory: () => Option[String] =
    getToken(gatewayUser1WithRequiredRole, gatewayUser1Password)
  private val invalidTokenFactory: () => Option[String] =
    getToken(gatewayUser2WithoutRequiredRole, gatewayUser2Password)

  private def getToken(tokenUserName: String, tokenPassword: String) = { () =>
    Some(
      BearerToken
        .fromServer(
          host = Networks().hostname,
          port = keycloakPort,
          username = tokenUserName,
          password = tokenPassword,
          realm = "TMT-test",
          client = "esw-gateway-client"
        )
        .token
    )
  }

  private var keycloakStopHandle: StopHandle = _

  private val mockResolver: Resolver = mock[Resolver]

  private val mockCommandService: CommandService = mock[CommandService]
  private val componentIdCommandService          = ComponentId(Prefix("IRIS.filter.wheel"), Assembly)
  private val startExposureCommand               = Setup(Prefix("CSW.ncc.trombone"), CommandName("startExposure"), Some(ObsId("obsId")))

  private val mockSequencerCommandService = mock[SequencerApi]
  private val componentIdSequencer        = ComponentId(Prefix("IRIS.MoonNight"), ComponentType.Sequencer)
  private val sequence                    = Sequence(Setup(Prefix("CSW.ncc.trombone"), CommandName("startExposure"), Some(ObsId("obsId"))))

  private var gatewayServerWiring: GatewayWiring = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    keycloakStopHandle = startKeycloak()
    gatewayServerWiring = startGateway()

    when(mockResolver.commandService(componentIdCommandService)).thenReturn(Future.successful(mockCommandService))
    when(mockCommandService.submit(startExposureCommand)).thenReturn(Future.successful(Started(Id("1234"))))

    when(mockResolver.sequencerCommandService(componentIdSequencer)).thenReturn(Future.successful(mockSequencerCommandService))
    when(mockSequencerCommandService.submit(sequence)).thenReturn(Future.successful(Started(Id("5678"))))
  }

  override def afterAll(): Unit = {
    keycloakStopHandle.stop()
    gatewayServerWiring.httpService.shutdown(UnknownReason).futureValue
    super.afterAll()
  }

  "Gateway" must {

    "return 200 response for protected command route with required role | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(validTokenFactory)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val commandService            = clientFactory.component(componentIdCommandService)

      val submitResponse = Await.result(commandService.submit(startExposureCommand), 10.minutes)
      submitResponse shouldBe a[Started]
    }

    "return 403 response for protected command route without required role | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(invalidTokenFactory)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val commandService            = clientFactory.component(componentIdCommandService)

      val httpError = intercept[HttpError](Await.result(commandService.submit(startExposureCommand), defaultTimeout))
      httpError.statusCode shouldBe 403
    }

    "return 200 response for protected sequencer route with required role | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(validTokenFactory)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val sequencer                 = clientFactory.sequencer(componentIdSequencer)

      val submitResponse = Await.result(sequencer.submit(sequence), 10.minutes)
      submitResponse shouldBe a[Started]
    }

    "return 403 response for protected sequencer route without required role | ESW-95" in {
      val gatewayPostClientWithAuth = gatewayHTTPClient(invalidTokenFactory)
      val clientFactory             = new ClientFactory(gatewayPostClientWithAuth, gatewayWsClient)
      val sequencer                 = clientFactory.sequencer(componentIdSequencer)

      val httpError = intercept[HttpError](Await.result(sequencer.submit(sequence), defaultTimeout))
      httpError.statusCode shouldBe 403
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
              gatewayUser1WithRequiredRole,
              gatewayUser1Password,
              realmRoles = Set(irisUserRole)
            ),
            ApplicationUser(
              gatewayUser2WithoutRequiredRole,
              gatewayUser2Password
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
    val gatewayWiring = new GatewayWiring(Some(SocketUtils.getFreePort), local = true, commandRolesPath) {
      override val resolver: Resolver = mockResolver
    }
    Await.result(gatewayWiring.httpService.registeredLazyBinding, defaultTimeout)
    gatewayWiring
  }

}
