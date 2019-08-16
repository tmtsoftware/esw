package esw.gateway.server.routes.restless

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import akka.util.Timeout
import akka.{Done, NotUsed}
import csw.alarm.api.exceptions.KeyNotFoundException
import csw.alarm.models.Key.AlarmKey
import csw.event.api.scaladsl.{EventPublisher, EventSubscriber}
import csw.location.models.ComponentType
import csw.params.commands.CommandResponse.Error
import csw.params.core.models.Id
import csw.params.events.EventKey
import esw.gateway.server.routes.restless.CommandAction.{Oneway, Submit, Validate}
import esw.gateway.server.routes.restless.RequestMsg.{CommandMsg, GetEventMsg, PublishEventMsg, SetAlarmSeverityMsg}
import esw.gateway.server.routes.restless.ResponseMsg.{NoEventKeys, SetAlarmSeverityFailure}
import esw.http.core.utils.CswContext
import msocket.core.api.Payload
import msocket.core.api.ToResponse.FutureToPayload
import msocket.core.server.ServerSocket

import scala.concurrent.duration.DurationLong
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class RestlessRoutes(cswCtx: CswContext) extends RestlessCodecs {

  import cswCtx._
  implicit val timeout: Timeout = Timeout(5.seconds)
  import actorRuntime.typedSystem.executionContext

  lazy val subscriber: EventSubscriber = eventService.defaultSubscriber
  lazy val publisher: EventPublisher   = eventService.defaultPublisher

  val route1: Route = post {
    path("gateway") {
      entity(as[RequestMsg]) {
        case CommandMsg(componentType, name, command, action) =>
          val commandServiceF = componentFactory.commandService(name, componentType)
          val eventualCommandResponse = commandServiceF
            .flatMap { commandService =>
              action match {
                case Oneway   => commandService.oneway(command)
                case Submit   => commandService.submit(command)
                case Validate => commandService.validate(command)
              }
            }
            .recover {
              case NonFatal(ex) => Error(command.runId, ex.getMessage)
            }
          complete(eventualCommandResponse)

        case PublishEventMsg(event) => complete(publisher.publish(event))
        case GetEventMsg(keys) =>
          if (keys.nonEmpty) complete(subscriber.get(keys.toEventKeys))
          else complete(NoEventKeys)

        case SetAlarmSeverityMsg(subsystem, componentName, alarmName, severity) =>
          onComplete(alarmService.setSeverity(AlarmKey(subsystem, componentName, alarmName), severity)) {
            case Success(_)                       => complete(Done)
            case Failure(e: KeyNotFoundException) => complete(SetAlarmSeverityFailure(e.getMessage))
            case Failure(ex)                      => throw ex
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
        componentFactory
          .commandService(componentName, componentType)
          .flatMap(_.queryFinal(runId)(Timeout(100.hours)))
          .recover {
            case ex: Exception =>
              Error(runId, ex.getMessage) //Could be a separate error like "InvalidComponent"
          }
          .payload
    }
  }

//  val route2 = post {
//    path("websocket" / Segment) { encoding =>
//      handleWebSocketMessages()
//    }
//  }

  implicit class RichEventKeys(keys: Iterable[String]) {
    def toEventKeys: Set[EventKey] = keys.map(EventKey(_)).toSet
  }

}
