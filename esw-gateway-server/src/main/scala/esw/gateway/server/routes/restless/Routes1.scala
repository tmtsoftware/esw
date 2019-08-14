package esw.gateway.server.routes.restless

import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Source
import akka.util.Timeout
import akka.{Done, NotUsed}
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.event.api.scaladsl.{EventPublisher, EventSubscriber}
import csw.location.client.HttpCodecs
import csw.location.models.ComponentType
import csw.params.commands.CommandResponse.Error
import csw.params.commands.ControlCommand
import csw.params.core.formats.ParamCodecs
import csw.params.core.models.Id
import csw.params.events.{Event, EventKey}
import esw.gateway.server.routes.restless.ResponseMsg.{CommandActionFailure, NoEventKeys, SetAlarmSeverityFailure}
import esw.gateway.server.routes.restless.RoutesMsg.{CommandMsg, GetEventMsg, PublishEventMsg, SetAlarmSeverityMsg}
import esw.http.core.msocket.api.Payload
import esw.http.core.msocket.api.ToResponse.FutureToPayload
import esw.http.core.msocket.server.ServerSocket
import esw.http.core.utils.CswContext

import scala.concurrent.duration.DurationLong

trait Action
object ActionMsg {
  case object Validate extends Action
  case object Submit   extends Action
  case object Oneway   extends Action
}

trait RouteMsg
object RoutesMsg {

  case class CommandMsg(componentType: ComponentType, componentName: String, command: ControlCommand, action: Action)
      extends RouteMsg

  case class PublishEventMsg(event: Event)                                    extends RouteMsg
  case class GetEventMsg(keys: Set[String])                                   extends RouteMsg
  case class SetAlarmSeverityMsg(alarmKey: AlarmKey, severity: AlarmSeverity) extends RouteMsg
}

sealed trait ResponseMsg {
  def msg: String
}
object ResponseMsg {

  case object NoEventKeys extends ResponseMsg {
    val msg: String = "Request is missing query parameter key"
  }

  case class SetAlarmSeverityFailure(msg: String) extends ResponseMsg
  case class CommandActionFailure(msg: String)    extends ResponseMsg
}

class Routes1(cswCtx: CswContext) extends RestlessCodecs with ParamCodecs with HttpCodecs {

  import cswCtx._
  implicit val timeout: Timeout = Timeout(5.seconds)
  import actorRuntime.typedSystem.executionContext

  lazy val subscriber: EventSubscriber = eventService.defaultSubscriber
  lazy val publisher: EventPublisher   = eventService.defaultPublisher

  val route1 = post {
    path("gateway") {
      entity(as[RouteMsg]) {
        case CommandMsg(componentType, name, command, action) => {
          val commandServiceF = componentFactory.commandService(name, componentType)
          val eventualObject = commandServiceF
            .flatMap { commandService =>
              action match {
                case ActionMsg.Oneway   => commandService.oneway(command)
                case ActionMsg.Submit   => commandService.submit(command)
                case ActionMsg.Validate => commandService.validate(command)
              }
            }
            .recover {
              case ex: Exception => Error(command.runId, ex.getMessage)
            }
          complete(eventualObject)
        }

        case PublishEventMsg(event) => complete(publisher.publish(event))
        case GetEventMsg(keys) => {
          if (keys.nonEmpty) complete(subscriber.get(keys.toEventKeys))
          else complete(NoEventKeys)
        }

        case SetAlarmSeverityMsg(alarmKey, severity) =>
          alarmService
            .setSeverity(alarmKey, severity)
            .map(_ => complete(Done))
            .recover {
              case ex: Exception => complete(SetAlarmSeverityFailure(ex.getMessage))
            }
      }
    }
  }

  trait WebSocketMsg
  object WebSocketMsg {
    case class QueryCommandMsg(componentType: ComponentType, componentName: String, runId: Id) extends WebSocketMsg
  }

  import WebSocketMsg._
  val socket: ServerSocket[WebSocketMsg] = new ServerSocket[WebSocketMsg] {
    override def requestStream(request: WebSocketMsg): Source[Payload[_], NotUsed] = request match {
      case QueryCommandMsg(componentType, componentName, runId) =>
        val commandServiceF = componentFactory
          .commandService(componentName, componentType)
          .flatMap(_.queryFinal(runId)(Timeout(100.hours)))
          .recover {
            case ex: Exception =>
              CommandActionFailure(ex.getMessage) //Could be a separate error like "InvalidComponent"
          }
          .payload
    }
  }

  val route2 = post {
    path("websocket" / Segment) { encoding =>
      handleWebSocketMessages()
    }
  }

  implicit class RichEventKeys(keys: Iterable[String]) {
    def toEventKeys: Set[EventKey] = keys.map(EventKey(_)).toSet
  }

}
