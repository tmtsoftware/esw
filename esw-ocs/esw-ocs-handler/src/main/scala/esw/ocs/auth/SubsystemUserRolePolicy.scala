package esw.ocs.auth

import csw.aas.http.AuthorizationPolicy.CustomPolicy
import csw.command.client.auth.Roles
import csw.prefix.models.Subsystem

object SubsystemUserRolePolicy {
  def apply(subsystem: Subsystem): CustomPolicy = CustomPolicy { token =>
    Roles(token.realm_access.roles).containsUserRole(subsystem)
  }
}
