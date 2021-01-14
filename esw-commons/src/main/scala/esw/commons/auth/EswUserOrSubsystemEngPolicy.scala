package esw.commons.auth

import csw.aas.http.AuthorizationPolicy.CustomPolicy
import csw.aas.http.Roles
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.ESW

object EswUserOrSubsystemEngPolicy {
  def apply(subsystem: Subsystem): CustomPolicy =
    CustomPolicy(token => {
      val roles = Roles(token.realm_access.roles)
      roles.containsUserRole(ESW) || roles.containsEngRole(subsystem)
    })
}
