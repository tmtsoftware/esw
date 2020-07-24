package esw.sm.auth

import csw.aas.http.AuthorizationPolicy.CustomPolicy
import csw.command.client.auth.Roles
import csw.prefix.models.Subsystem.ESW

object EswUserRolePolicy {
  def apply(): CustomPolicy = CustomPolicy(token => Roles(token.realm_access.roles).containsUserRole(ESW))
}
