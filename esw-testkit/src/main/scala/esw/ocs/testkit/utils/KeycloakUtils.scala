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

  lazy val gatewayUser1                                    = "gateway-user1"
  lazy val gatewayUser1Password                            = "gateway-user1"
  lazy val gatewayUser2                                    = "gateway-user2"
  lazy val gatewayUser2Password                            = "gateway-user2"
  lazy val serverTimeout: FiniteDuration                   = 3.minutes
  lazy val keycloakPort: Int                               = SocketUtils.getFreePort
  lazy val tokenWithRequiredRoles: () => Option[String]    = getToken(gatewayUser1, gatewayUser1Password)
  lazy val tokenWithoutRequiredRoles: () => Option[String] = getToken(gatewayUser2, gatewayUser2Password)

  lazy val irisUserRole = "IRIS-user"
  lazy val irisEngRole  = "IRIS-eng"

  private lazy val `esw-gateway-server`: Client = Client(
    "esw-gateway-server",
    "bearer-only"
  )

  private lazy val `esw-gateway-client`: Client =
    Client("esw-gateway-client", "public", passwordGrantEnabled = true, authorizationEnabled = false)

  private lazy val userWithIrisEngAndIrisUserRole = ApplicationUser(
    gatewayUser1,
    gatewayUser1Password,
    realmRoles = Set(irisUserRole, irisEngRole)
  )
  private lazy val userWithoutAnyRole = ApplicationUser(
    gatewayUser2,
    gatewayUser2Password
  )
  private lazy val users = Set(
    userWithIrisEngAndIrisUserRole,
    userWithoutAnyRole
  )
  private lazy val defaultGatewayData: KeycloakData = KeycloakData(
    AdminUser("admin", "admin"),
    realms = Set(
      Realm(
        "TMT-test",
        clients = Set(`esw-gateway-server`, `esw-gateway-client`),
        users = users,
        realmRoles = Set(irisUserRole, irisEngRole)
      )
    )
  )

  private var keycloakStopHandle: Option[StopHandle] = None

  def startKeycloak(keycloakData: KeycloakData = defaultGatewayData): StopHandle = {
    val embeddedKeycloak =
      new EmbeddedKeycloak(keycloakData, KeycloakSettings(port = keycloakPort, printProcessLogs = false))
    val stopHandle = Await.result(embeddedKeycloak.startServer()(actorSystem.executionContext), serverTimeout)
    Await.result(locationService.register(HttpRegistration(AASConnection.value, keycloakPort, "auth")), defaultTimeout)
    keycloakStopHandle = Some(stopHandle)
    stopHandle
  }

  def stopKeycloak(): Unit = {
    keycloakStopHandle.foreach(_.stop())
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
