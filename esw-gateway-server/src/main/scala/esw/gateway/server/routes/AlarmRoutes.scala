package esw.gateway.server.routes

import akka.Done
import akka.http.javadsl.server.Rejections
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.alarm.api.exceptions.KeyNotFoundException
import csw.alarm.models.Key.AlarmKey
import csw.location.client.HttpCodecs
import csw.params.core.formats.ParamCodecs
import csw.params.core.models.Subsystem
import esw.gateway.server.requests.SetSeverity
import esw.http.core.utils.CswContext

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class AlarmRoutes(cswContext: CswContext) extends ParamCodecs with HttpCodecs {

  import SetSeverity._
  import cswContext.alarmService._

  val route: Route = pathPrefix("alarm") {
    pathPrefix("current-severity") {
      (post & path(Segment / Segment / Segment)) { (subsystemName, componentName, alarmName) =>
        entity(as[SetSeverity]) { body =>
          val subsystemMaybe = Try { Subsystem.withNameInsensitive(subsystemName) }
          subsystemMaybe match {
            case Failure(exception) => reject(Rejections.validationRejection(exception.getMessage))
            case Success(subsystem) =>
              onComplete(setSeverity(AlarmKey(subsystem, componentName, alarmName), body.severity)) {
                case Success(_)                       => complete(Done)
                case Failure(e: KeyNotFoundException) => reject(Rejections.validationRejection(e.getMessage))
                case Failure(NonFatal(e))             => throw e //this will return 500
              }
          }
        }
      }
    }
  }
}
