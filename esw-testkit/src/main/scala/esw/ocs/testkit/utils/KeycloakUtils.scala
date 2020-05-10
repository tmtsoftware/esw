package esw.ocs.testkit.utils

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.aas.core.commons.AASConnection
import csw.location.api.models.HttpRegistration
import csw.location.api.scaladsl.LocationService
import csw.network.utils.{Networks, SocketUtils}
import org.tmt.embedded_keycloak.KeycloakData.{AdminUser, ApplicationUser, Client, Realm}
import org.tmt.embedded_keycloak.impl.StopHandle
import org.tmt.embedded_keycloak.utils.BearerToken
import org.tmt.embedded_keycloak.{EmbeddedKeycloak, KeycloakData, Settings => KeycloakSettings}

import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait KeycloakUtils extends BaseTestSuite {
  def locationService: LocationService
  implicit def actorSystem: ActorSystem[SpawnProtocol.Command]

  val gatewayUser1WithRequiredRole    = "gateway-user1"
  val gatewayUser1Password            = "gateway-user1"
  val gatewayUser2WithoutRequiredRole = "gateway-user2"
  val gatewayUser2Password            = "gateway-user2"
  val serverTimeout: FiniteDuration   = 3.minutes
  val keycloakPort: Int               = SocketUtils.getFreePort
  val validTokenFactory: () => Option[String] =
    getToken(gatewayUser1WithRequiredRole, gatewayUser1Password)
  val invalidTokenFactory: () => Option[String] =
    getToken(gatewayUser2WithoutRequiredRole, gatewayUser2Password)

  val irisUserRole = "IRIS-user"

  private val `esw-gateway-server`: Client = Client(
    "esw-gateway-server",
    "bearer-only"
  )

  private val `esw-gateway-client`: Client =
    Client("esw-gateway-client", "public", passwordGrantEnabled = true, authorizationEnabled = false)

  private val users = Set(
    ApplicationUser(
      gatewayUser1WithRequiredRole,
      gatewayUser1Password,
      realmRoles = Set(irisUserRole)
    ),
    ApplicationUser(
      gatewayUser2WithoutRequiredRole,
      gatewayUser2Password
    )
  )
  private val defaultGatewayData: KeycloakData = KeycloakData(
    AdminUser("admin", "admin"),
    realms = Set(
      Realm(
        "TMT-test",
        clients = Set(`esw-gateway-server`, `esw-gateway-client`),
        users = users,
        realmRoles = Set(irisUserRole)
      )
    )
  )

  def startKeycloak(keycloakData: KeycloakData = defaultGatewayData): StopHandle = {
    val embeddedKeycloak =
      new EmbeddedKeycloak(keycloakData, KeycloakSettings(port = keycloakPort, printProcessLogs = false))
    val stopHandle = Await.result(embeddedKeycloak.startServer()(actorSystem.executionContext), serverTimeout)
    Await.result(locationService.register(HttpRegistration(AASConnection.value, keycloakPort, "auth")), defaultTimeout)
    stopHandle
  }

  def getToken(tokenUserName: String, tokenPassword: String, client: String = "esw-gateway-client") = { () =>
    Some(
      BearerToken
        .fromServer(
          host = Networks().hostname,
          port = keycloakPort,
          username = tokenUserName,
          password = tokenPassword,
          realm = "TMT-test",
          client = client
        )
        .token
    )
  }
}
