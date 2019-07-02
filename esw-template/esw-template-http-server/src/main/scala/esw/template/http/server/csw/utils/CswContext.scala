package esw.template.http.server.csw.utils

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.command.client.CommandServiceFactory
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.internal.commons.EventSubscriberUtil
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import esw.template.http.server.commons.ServiceLogger
import esw.template.http.server.wiring.{ActorRuntime, Settings}

class CswContext(_port: Option[Int]) {
  lazy val settings                                = new Settings(_port)
  lazy val actorSystem: ActorSystem[SpawnProtocol] = ActorSystemFactory.remote(SpawnProtocol.behavior, "http-server")
  lazy val actorRuntime: ActorRuntime              = new ActorRuntime(actorSystem)

  import actorRuntime._

  lazy val logger: Logger = new ServiceLogger(settings.httpConnection).getLogger

  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient(actorSystem, actorRuntime.mat)

  lazy val eventService: EventService               = new EventServiceFactory().make(locationService)
  lazy val eventSubscriberUtil: EventSubscriberUtil = new EventSubscriberUtil()

  lazy val componentFactory = new ComponentFactory(locationService, CommandServiceFactory)
}
