package esw.http.core.wiring

import com.typesafe.config.ConfigFactory
import csw.aas.http.SecurityDirectives
import csw.location.api.models.ComponentType
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix

class ServerWiring(_port: Option[Int], prefix: Option[Prefix] = None) {
  private lazy val config        = ConfigFactory.load()
  lazy val settings              = new Settings(_port, prefix, config, ComponentType.Service)
  lazy val cswWiring             = new CswWiring()
  private lazy val loggerFactory = new LoggerFactory(settings.httpConnection.prefix)
  lazy val logger: Logger        = loggerFactory.getLogger
  lazy val securityDirectives: SecurityDirectives =
    SecurityDirectives(config, cswWiring.locationService)(cswWiring.actorRuntime.ec)
}
