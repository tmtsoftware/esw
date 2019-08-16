package esw.gateway.server.routes.restless

import akka.Done
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import csw.alarm.api.exceptions.KeyNotFoundException
import csw.alarm.models.Key.AlarmKey
import esw.gateway.server.routes.restless.codecs.RestlessCodecs
import esw.gateway.server.routes.restless.impl.{CommandServiceImpl, EventServiceImpl}
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg.SetAlarmSeverityFailure
import esw.gateway.server.routes.restless.messages.RequestMsg
import esw.gateway.server.routes.restless.messages.RequestMsg.{CommandMsg, GetEventMsg, PublishEventMsg, SetAlarmSeverityMsg}
import esw.http.core.utils.CswContext

import scala.concurrent.duration.DurationLong
import scala.util.{Failure, Success}

class HttpRoute(cswCtx: CswContext) extends RestlessCodecs {

  lazy val commandServiceApi: CommandServiceImpl = new CommandServiceImpl(cswCtx)
  lazy val eventServiceApi: EventServiceImpl     = new EventServiceImpl(cswCtx)
  import cswCtx._
  implicit val timeout: Timeout = Timeout(5.seconds)

  val route: Route = post {
    path("gateway") {
      entity(as[RequestMsg]) {
        case commandMsg: CommandMsg           => complete(commandServiceApi.process(commandMsg))
        case publishEventMsg: PublishEventMsg => complete(eventServiceApi.publish(publishEventMsg))
        case getEventMsg: GetEventMsg         => complete(eventServiceApi.get(getEventMsg))
        case SetAlarmSeverityMsg(subsystem, componentName, alarmName, severity) =>
          onComplete(alarmService.setSeverity(AlarmKey(subsystem, componentName, alarmName), severity)) {
            case Success(_)                       => complete(Done)
            case Failure(e: KeyNotFoundException) => complete(SetAlarmSeverityFailure(e.getMessage))
            case Failure(ex)                      => throw ex
          }
      }
    }
  }

}
