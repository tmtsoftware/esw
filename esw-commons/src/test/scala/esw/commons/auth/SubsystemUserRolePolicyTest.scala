package esw.commons.auth

import csw.prefix.models.Subsystem
import msocket.security.models.{Access, AccessToken}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SubsystemUserRolePolicyTest extends AnyFunSuite with Matchers {

  test("should grant access if subsystem user role present in token | ESW-95") {
    val token = AccessToken(realm_access = Access(Set("TCS-user")))
    SubsystemUserRolePolicy(Subsystem.TCS).predicate(token) shouldBe true
  }

  test("should not grant access if subsystem user role not present in token | ESW-95") {
    val token = AccessToken(realm_access = Access(Set("APS-user")))
    SubsystemUserRolePolicy(Subsystem.TCS).predicate(token) shouldBe false
  }
}
