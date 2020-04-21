package esw.ocs.auth

import csw.aas.core.token.AccessToken
import csw.aas.http.AuthorizationPolicy.CustomPolicy
import csw.command.client.auth.Roles
import csw.prefix.models.Subsystem

object SubsystemUserRolePolicy {
  def apply(subsystem: Subsystem): CustomPolicy = {
    CustomPolicy { token => hasAccess(token, subsystem) }
  }

  private[auth] def hasAccess(token: AccessToken, subsystem: Subsystem) = {
    Roles(token.realm_access.roles).containsUserRole(subsystem)
  }
}
