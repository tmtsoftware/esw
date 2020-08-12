package esw.agent.service.app.auth

import csw.aas.http.AuthorizationPolicy.CustomPolicy
import csw.command.client.auth.Roles
import csw.prefix.models.Subsystem.ESW

//todo: reuse the policy defined in sm
object EswUserRolePolicy {
  def apply(): CustomPolicy = CustomPolicy(token => Roles(token.realm_access.roles).containsUserRole(ESW))
}
