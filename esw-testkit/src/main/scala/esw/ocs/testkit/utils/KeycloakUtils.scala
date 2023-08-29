package esw.ocs.testkit.utils

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
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

  lazy val gatewayRoleIrisUserIrisEng                          = "gateway-user1"
  lazy val gatewayUser1Password                                = "gateway-user1"
  lazy val gatewayRoleTcsUser                                  = "gateway-user2"
  lazy val gatewayUser2Password                                = "gateway-user2"
  lazy val gatewayRoleIrisUser                                 = "gateway-user3"
  lazy val gatewayUser3Password                                = "gateway-user3"
  lazy val gatewayRoleApsEng                                   = "gateway-user4"
  lazy val gatewayUser4Password                                = "gateway-user4"
  lazy val locationAdminUser                                   = "location-admin1"
  lazy val locationAdminPassword                               = "location-admin1"
  lazy val smRoleEswUserEng                                    = "sm-user1"
  lazy val smUser1Password                                     = "sm-user1"
  lazy val serverTimeout: FiniteDuration                       = 3.minutes
  lazy val keycloakPort: Int                                   = SocketUtils.getFreePort
  lazy val tokenWithIrisUserIrisEngRoles: () => Option[String] = getToken(gatewayRoleIrisUserIrisEng, gatewayUser1Password)
  lazy val tokenWithTcsUserRole: () => Option[String]          = getToken(gatewayRoleTcsUser, gatewayUser2Password)
  lazy val tokenWithIrisUserRole: () => Option[String]         = getToken(gatewayRoleIrisUser, gatewayUser3Password)
  lazy val tokenWithApsEngRole: () => Option[String]           = getToken(gatewayRoleApsEng, gatewayUser4Password)
  lazy val tokenWithEswUserRole: () => Option[String]          = getToken(smRoleEswUserEng, smUser1Password)

  lazy val irisUserRole = "IRIS-user"
  lazy val irisEngRole  = "IRIS-eng"
  lazy val apsEngRole   = "APS-eng"
  lazy val tcsUserRole  = "TCS-user"
  lazy val eswUserRole  = "ESW-user"
  lazy val locAdminRole = "location-admin"

  private lazy val frontEndClientId = "tmt-frontend-app"

  private lazy val `tmt-frontend-app`: Client =
    Client(frontEndClientId, "public", implicitFlowEnabled = true, passwordGrantEnabled = true, authorizationEnabled = false)

  private lazy val userWithEswUserRole = ApplicationUser(
    smRoleEswUserEng,
    smUser1Password,
    realmRoles = Set(eswUserRole)
  )

  private lazy val userWithIrisEngAndIrisUserRole = ApplicationUser(
    gatewayRoleIrisUserIrisEng,
    gatewayUser1Password,
    realmRoles = Set(irisUserRole, irisEngRole)
  )

  private lazy val userWithIrisUserRole = ApplicationUser(
    gatewayRoleIrisUser,
    gatewayUser3Password,
    realmRoles = Set(irisUserRole)
  )

  private lazy val userWithTcsUserRole = ApplicationUser(
    gatewayRoleTcsUser,
    gatewayUser2Password,
    realmRoles = Set(tcsUserRole)
  )

  private lazy val userWithApsEngRole = ApplicationUser(
    gatewayRoleApsEng,
    gatewayUser4Password,
    realmRoles = Set(apsEngRole)
  )

  private lazy val userWithLocAdminRole = ApplicationUser(
    locationAdminUser,
    locationAdminPassword,
    realmRoles = Set(locAdminRole)
  )

  private lazy val users = Set(
    userWithIrisEngAndIrisUserRole,
    userWithTcsUserRole,
    userWithIrisUserRole,
    userWithApsEngRole,
    userWithEswUserRole,
    userWithLocAdminRole
  )

  private lazy val defaultGatewayData: KeycloakData = KeycloakData(
    AdminUser("admin", "admin"),
    realms = Set(
      Realm(
        "TMT",
        clients = Set(`tmt-frontend-app`),
        users = users,
        realmRoles = Set(irisUserRole, irisEngRole, tcsUserRole, apsEngRole, eswUserRole, locAdminRole)
      )
    )
  )

  private var keycloakStopHandle: Option[StopHandle] = None

  def startKeycloak(keycloakData: KeycloakData = defaultGatewayData): StopHandle = {
    val embeddedKeycloak =
      new EmbeddedKeycloak(keycloakData, KeycloakSettings(port = keycloakPort, printProcessLogs = false))
    val stopHandle = Await.result(embeddedKeycloak.startServer()(actorSystem.executionContext), serverTimeout)
    registerKeycloak()
    keycloakStopHandle = Some(stopHandle)
    stopHandle
  }

  private[esw] def registerKeycloak() =
    Await.result(locationService.register(HttpRegistration(AASConnection.value, keycloakPort, "")), defaultTimeout)

  def stopKeycloak(): Unit = keycloakStopHandle.foreach(_.stop())

  def getToken(tokenUserName: String, tokenPassword: String, client: String = frontEndClientId): () => Some[String] = { () =>
    Some(
      BearerToken
        .fromServer(
          host = Networks().hostname,
          port = keycloakPort,
          username = tokenUserName,
          password = tokenPassword,
          realm = "TMT",
          client = client
        )
        .token
    )
  }
}
