package esw.gateway.server2

import akka.util.Timeout
import esw.gateway.api.{AlarmServiceApi, CommandServiceApi, EventServiceApi}
import esw.gateway.impl.{AlarmServiceImpl, CommandServiceImpl, EventServiceImpl}
import esw.http.core.utils.CswContext

import scala.concurrent.duration.DurationInt

class GatewayContext(val cswContext: CswContext) {

  import cswContext._
  import cswContext.actorRuntime.ec

  implicit private val timeout: Timeout = 10.seconds

  val alarmServiceApi: AlarmServiceApi     = new AlarmServiceImpl(alarmService)
  val commandServiceApi: CommandServiceApi = new CommandServiceImpl(componentFactory)
  val eventServiceApi: EventServiceApi     = new EventServiceImpl(eventService, eventSubscriberUtil)
}
