package csw.gateway.mock

import csw.aas.http.{PolicyValidator, SecurityDirectives}
import esw.testcommons.BaseTestSuite
import msocket.security.api.TokenValidator
import msocket.security.models.{Access, AccessToken, SecurityStatus}
import msocket.security.{AccessController, AccessControllerFactory}
import org.mockito.ArgumentMatchers.{any, eq => argsEq}
import org.mockito.Mockito.when

import scala.concurrent.{ExecutionContext, Future}

class AuthMocks(implicit ec: ExecutionContext) extends BaseTestSuite {

  private val realmRole                                = "TMT"
  val accessControllerFactory: AccessControllerFactory = mock[AccessControllerFactory]
  private val policyValidator                          = new PolicyValidator(accessControllerFactory, realmRole)
  val securityDirectives                               = new SecurityDirectives(policyValidator)

  val token: String                  = randomString(10)
  val tokenOpt: Some[String]         = Some(token)
  val tokenValidator: TokenValidator = mock[TokenValidator]
  private val accessController       = new AccessController(tokenValidator, SecurityStatus.from(tokenOpt, securityEnabled = true))

  when(accessControllerFactory.make(argsEq(tokenOpt))(any[ExecutionContext]())).thenReturn(accessController)

  val accessToken: AccessToken = mock[AccessToken]
  when(tokenValidator.validate(token)).thenReturn(Future.successful(accessToken))

  val access: Access = mock[Access]
  when(accessToken.realm_access).thenReturn(access)

  def stubRolesInAccessToken(roles: Set[String]): Unit = {
    when(access.roles).thenReturn(roles)
  }

}
