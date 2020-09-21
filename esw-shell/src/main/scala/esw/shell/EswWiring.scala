package esw.shell

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.framework.CswWiring
import esw.commons.utils.location.LocationServiceUtil
import esw.gateway.api.AdminApi
import esw.gateway.impl.AdminImpl

class EswWiring {
  lazy val cswWiring = new CswWiring

  private implicit lazy val typedSystem: ActorSystem[SpawnProtocol.Command] = cswWiring.wiring.actorSystem

  private lazy val locationUtils = new LocationServiceUtil(cswWiring.cswContext.locationService)

  lazy val factories          = new Factories(locationUtils)
  lazy val adminApi: AdminApi = new AdminImpl(cswWiring.cswContext.locationService)
}
