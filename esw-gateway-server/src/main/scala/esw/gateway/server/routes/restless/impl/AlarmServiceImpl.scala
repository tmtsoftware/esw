package esw.gateway.server.routes.restless.impl

import akka.Done
import csw.alarm.models.Key.AlarmKey
import esw.gateway.server.routes.restless.api.AlarmServiceApi
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg.SetAlarmSeverityFailure
import esw.gateway.server.routes.restless.messages.{ErrorResponseMsg, RequestMsg}
import esw.http.core.utils.CswContext

import scala.concurrent.Future
import scala.util.control.NonFatal

class AlarmServiceImpl(cswCtx: CswContext) extends AlarmServiceApi {
  import cswCtx._
  import actorRuntime.typedSystem.executionContext

  override def setSeverity(setAlarmSeverityMsg: RequestMsg.SetAlarmSeverityMsg): Future[Either[ErrorResponseMsg, Done]] = {
    import setAlarmSeverityMsg._
    alarmService
      .setSeverity(AlarmKey(subsystem, componentName, alarmName), severity)
      .map(Right(_))
      .recover {
        case NonFatal(e) => Left(SetAlarmSeverityFailure(e.getMessage))
      }
  }
}
