package esw.template.http.server.csw.utils

import csw.command.client.CommandServiceFactory
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.internal.commons.EventSubscriberUtil
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import esw.template.http.server.commons.ServiceLogger
import esw.template.http.server.wiring.ActorRuntime

class CswContext(actorRuntime: ActorRuntime, httpConnection: HttpConnection) {
  import actorRuntime._

  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient(typedSystem, mat)

  lazy val eventSubscriberUtil: EventSubscriberUtil = new EventSubscriberUtil()
  lazy val eventService: EventService               = new EventServiceFactory().make(locationService)

  lazy val componentFactory = new ComponentFactory(locationService, CommandServiceFactory)

  lazy val logger: Logger = new ServiceLogger(httpConnection).getLogger
}
