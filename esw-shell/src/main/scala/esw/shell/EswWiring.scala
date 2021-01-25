package esw.shell

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.config.api.TokenFactory
import csw.config.api.scaladsl.ConfigService
import csw.config.client.scaladsl.ConfigClientFactory
import csw.framework.CswWiring
import csw.location.api.scaladsl.LocationService
import esw.commons.utils.location.LocationServiceUtil
import esw.shell.utils.Keycloak

class EswWiring {
  lazy val cswWiring = new CswWiring

  private implicit lazy val typedSystem: ActorSystem[SpawnProtocol.Command] = cswWiring.wiring.actorSystem

  private lazy val locationService: LocationService   = cswWiring.cswContext.locationService
  private lazy val locationUtils: LocationServiceUtil = new LocationServiceUtil(locationService)
  private lazy val configService: ConfigService       = ConfigClientFactory.adminApi(typedSystem, locationService, tokenFactory)

  lazy val factories = new Factories(locationUtils, configService)

  private lazy val config                      = ConfigFactory.load().getConfig("csw")
  private lazy val configAdminUsername: String = config.getString("configAdminUsername")
  private lazy val configAdminPassword: String = config.getString("configAdminPassword")

  private def tokenFactory: TokenFactory =
    new TokenFactory {
      override def getToken: String = Keycloak.getToken(configAdminUsername, configAdminPassword)
    }
}
