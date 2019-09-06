package esw.http.core.utils

import com.typesafe.config.Config
import csw.aas.http.SecurityDirectives
import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.client.AlarmServiceFactory
import csw.command.client.CommandServiceFactory
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.internal.commons.EventSubscriberUtil
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.HttpConnection
import csw.logging.api.scaladsl.Logger
import esw.http.core.commons.ServiceLogger
import esw.http.core.wiring.ActorRuntime

class CswContext(val actorRuntime: ActorRuntime, httpConnection: HttpConnection, config: Config) {
  import actorRuntime._

  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient(typedSystem, mat)

  lazy val eventSubscriberUtil: EventSubscriberUtil = new EventSubscriberUtil()

  lazy val eventService: EventService = new EventServiceFactory().make(locationService)

  lazy val componentFactory = new ComponentFactory(locationService, CommandServiceFactory)

  lazy val logger: Logger = new ServiceLogger(httpConnection).getLogger

  lazy val securityDirectives = SecurityDirectives(config, locationService)

  lazy val alarmService: AlarmService = new AlarmServiceFactory().makeClientApi(locationService)
}
