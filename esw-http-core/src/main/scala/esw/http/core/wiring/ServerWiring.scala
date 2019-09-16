package esw.http.core.wiring

import com.typesafe.config.ConfigFactory
import csw.aas.http.SecurityDirectives

class ServerWiring(_port: Option[Int], serviceName: Option[String] = None) {
  private lazy val config = ConfigFactory.load()
  lazy val settings       = new Settings(_port, serviceName, config)
  lazy val cswWiring      = new CswWiring(settings.httpConnection.componentId.name)
  lazy val securityDirectives: SecurityDirectives =
    SecurityDirectives(config, cswWiring.locationService)(cswWiring.actorRuntime.ec)
}
