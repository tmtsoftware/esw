package csw.backend.auth

import csw.aas.http.{Roles, SecurityDirectives}
import csw.command.client.auth.{CommandKey, CommandRoles}
import csw.prefix.models.Subsystem
import msocket.security.AccessControllerFactory
import msocket.security.api.TokenValidator
import msocket.security.models.{Access, AccessToken}
import org.keycloak.adapters.KeycloakDeployment
import org.mockito.{ArgumentMatchers, MockitoSugar}

import scala.concurrent.Future

class MockedAuth extends MockitoSugar {
  private val keycloakDeployment = new KeycloakDeployment()
  keycloakDeployment.setRealm("TMT")
  keycloakDeployment.setResourceName("tmt-backend-app")

  private val validTokenStr                      = "validToken"
  private val tokenWithoutRoleStr                = "tokenWithoutRole"
  private lazy val validToken: AccessToken       = mock[AccessToken]
  private lazy val tokenWithoutRole: AccessToken = mock[AccessToken]

  lazy val tokenValidator: TokenValidator = {
    case `validTokenStr`       => Future.successful(validToken)
    case `tokenWithoutRoleStr` => Future.successful(tokenWithoutRole)
    case token                 => Future.failed(new RuntimeException(s"unexpected token $token"))
  }

  val commandRoles: CommandRoles = new CommandRoles(Map.empty) {
    override def hasAccess(cmdKey: CommandKey, subsystem: Subsystem, rolesFromToken: Roles): Boolean = {
      rolesFromToken.roles == validRoles
    }
  }

  val _securityDirectives =
    new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), keycloakDeployment.getRealm)

  private val access: Access          = mock[Access]
  private val validRoles: Set[String] = mock[Set[String]]

  private val invalidAccess: Access     = mock[Access]
  private val invalidRoles: Set[String] = mock[Set[String]]

  when(validToken.realm_access).thenReturn(access)
  when(access.roles).thenReturn(validRoles)
  when(validRoles.map(ArgumentMatchers.any[String => String]())).thenReturn(validRoles)
  when(validRoles.contains(ArgumentMatchers.any[String]())).thenReturn(true)

  when(tokenWithoutRole.realm_access).thenReturn(invalidAccess)
  when(invalidAccess.roles).thenReturn(invalidRoles)
  when(invalidRoles.map(ArgumentMatchers.any[String => String]())).thenReturn(invalidRoles)
}
