package esw.ocs.auth

import csw.aas.core.token.AccessToken
import csw.aas.core.token.claims.Access
import csw.prefix.models.Subsystem
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SubsystemUserRolePolicyTest extends AnyFunSuite with Matchers {

  test("should grant access if subsystem user role present in token") {
    val token = AccessToken(realm_access = Access(Set("TCS-user")))
    SubsystemUserRolePolicy.hasAccess(token, Subsystem.TCS) shouldBe true
  }

  test("should not grant access if subsystem user role not present in token") {
    val accessToken = AccessToken(realm_access = Access(Set("APS-user")))
    SubsystemUserRolePolicy.hasAccess(accessToken, Subsystem.TCS) shouldBe false
  }
}
