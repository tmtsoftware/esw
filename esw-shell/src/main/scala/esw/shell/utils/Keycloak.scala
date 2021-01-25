package esw.shell.utils

import csw.network.utils.Networks
import org.tmt.embedded_keycloak.utils.BearerToken

object Keycloak {
  private val KeycloakPort = 8081
  private val hostname     = Networks().hostname
  private val Realm        = "TMT"
  private val Client       = "tmt-frontend-app"

  def getToken(userName: String, password: String): String = {
    BearerToken.fromServer(KeycloakPort, userName, password, Realm, Client, hostname).token
  }
}
