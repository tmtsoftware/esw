package esw.gateway.server.routes.restless

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import esw.gateway.server.routes.restless.codecs.RestlessCodecs
import esw.gateway.server.routes.restless.impl.{AlarmServiceImpl, CommandServiceImpl, EventServiceImpl}
import esw.gateway.server.routes.restless.messages.HttpRequestMsg
import esw.gateway.server.routes.restless.messages.HttpRequestMsg.{CommandMsg, GetEventMsg, PublishEventMsg, SetAlarmSeverityMsg}
import esw.http.core.utils.CswContext

class HttpRoute(cswCtx: CswContext) extends RestlessCodecs {

  lazy val commandServiceApi: CommandServiceImpl = new CommandServiceImpl(cswCtx)
  lazy val eventServiceApi: EventServiceImpl     = new EventServiceImpl(cswCtx)
  lazy val alarmServiceApi: AlarmServiceImpl     = new AlarmServiceImpl(cswCtx)

  val route: Route = post {
    path("gateway") {
      entity(as[HttpRequestMsg]) {
        case commandMsg: CommandMsg                   => complete(commandServiceApi.process(commandMsg))
        case publishEventMsg: PublishEventMsg         => complete(eventServiceApi.publish(publishEventMsg))
        case getEventMsg: GetEventMsg                 => complete(eventServiceApi.get(getEventMsg))
        case setAlarmSeverityMsg: SetAlarmSeverityMsg => complete(alarmServiceApi.setSeverity(setAlarmSeverityMsg))
      }
    }
  }

}
