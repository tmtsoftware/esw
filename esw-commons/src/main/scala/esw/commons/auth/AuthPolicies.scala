package esw.commons.auth

import csw.aas.http.AuthorizationPolicy.CustomPolicy
import csw.aas.http.Roles
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.ESW

/**
 * This is model representing Authorization policies defined for TMT.
 * There is an assumption that the user roles are appropriately defined in Keycloak server.
 * More information can be found here.
 * See <a href="https://tmtsoftware.github.io/esw/uisupport/gateway.html">Gateway documentation</a>
 */
object AuthPolicies {
  private def make(predicate: Roles => Boolean): CustomPolicy =
    CustomPolicy { token =>
      val roles = Roles(token.realm_access.roles)
      predicate(roles)
    }

  val eswUserRolePolicy: CustomPolicy =
    make(roles => roles.containsUserRole(ESW))

  def eswUserOrSubsystemUserPolicy(subsystem: Subsystem): CustomPolicy =
    make(roles => roles.containsUserRole(ESW) || roles.containsUserRole(subsystem))

  def eswUserOrSubsystemEngPolicy(subsystem: Subsystem): CustomPolicy =
    make(roles => roles.containsUserRole(ESW) || roles.containsEngRole(subsystem))

  def subsystemUserRolePolicy(subsystem: Subsystem): CustomPolicy =
    make(roles => roles.containsUserRole(subsystem))
}
