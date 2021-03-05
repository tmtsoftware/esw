package esw.commons.auth

import csw.aas.http.AuthorizationPolicy.CustomPolicy
import csw.aas.http.Roles
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.ESW

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
