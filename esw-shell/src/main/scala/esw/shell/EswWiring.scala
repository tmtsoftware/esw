package esw.shell

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.config.api.TokenFactory
import csw.config.api.scaladsl.ConfigService
import csw.config.client.scaladsl.ConfigClientFactory
import csw.framework.CswWiring
import csw.location.api.scaladsl.LocationService
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks
import esw.commons.extensions.FutureExt.FutureOps
import esw.commons.utils.aas.Keycloak
import esw.commons.utils.config.ConfigServiceExt
import esw.commons.utils.location.LocationServiceUtil
import esw.constants.CommonTimeouts

class EswWiring {
  final lazy val cswWiring = new CswWiring

  private implicit lazy val typedSystem: ActorSystem[SpawnProtocol.Command] = cswWiring.wiring.actorSystem

  private lazy val locationService: LocationService   = cswWiring.cswContext.locationService
  private lazy val locationUtils: LocationServiceUtil = new LocationServiceUtil(locationService)
  private lazy val configService: ConfigService       = ConfigClientFactory.adminApi(typedSystem, locationService, tokenFactory)
  private lazy val configServiceExt: ConfigServiceExt = new ConfigServiceExt(configService)

  final lazy val factories = new Factories(locationUtils, configServiceExt)

  private lazy val config                      = ConfigFactory.load().getConfig("csw")
  private lazy val configAdminUsername: String = config.getString("configAdminUsername")
  private lazy val configAdminPassword: String = config.getString("configAdminPassword")
  private lazy val keycloak                    = new Keycloak(locationService)(typedSystem.executionContext)

  def startLogging(name: String, version: String): Unit =
    LoggingSystemFactory.start(name, version, Networks().hostname, typedSystem)

  private def tokenFactory: TokenFactory =
    new TokenFactory {
      override def getToken: String = keycloak.getToken(configAdminUsername, configAdminPassword).await(CommonTimeouts.Wiring)
    }
}
