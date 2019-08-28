package esw.gateway.server2

import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.Timeout
import csw.alarm.api.exceptions.KeyNotFoundException
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.location.models.ComponentId
import csw.location.models.ComponentType.Assembly
import csw.params.commands.CommandResponse.{Accepted, Started}
import csw.params.commands.{CommandName, CommandResponse, ControlCommand, Setup}
import csw.params.core.models.{Id, ObsId, Prefix, Subsystem}
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.CommandAction.{Oneway, Submit, Validate}
import esw.gateway.api.messages.{EmptyEventKeys, InvalidComponent, PostRequest, SetAlarmSeverityFailure, WebsocketRequest}
import esw.gateway.api.messages.PostRequest.{CommandRequest, GetEvent, PublishEvent, SetAlarmSeverity}
import esw.gateway.api.{AlarmApi, CommandApi, EventApi}
import esw.gateway.impl.{AlarmImpl, CommandImpl, EventImpl}
import esw.http.core.BaseTestSuite
import mscoket.impl.{HttpCodecs, RoutesFactory}
import org.mockito.Mockito.when
import org.mockito.MockitoSugar._

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class PostHandlerImplTest extends BaseTestSuite with ScalatestRouteTest with RestlessCodecs with HttpCodecs {
  val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test-system")

  private val cswCtxMocks = new CswContextMocks(actorSystem)
  import cswCtxMocks._

  implicit val timeout: Timeout = Timeout(5.seconds)

  private val alarmApi: AlarmApi                                         = new AlarmImpl(alarmService)
  private val eventApi: EventApi                                         = new EventImpl(eventService, eventSubscriberUtil)
  private val commandApi: CommandApi                                     = new CommandImpl(componentFactory.commandService)
  private val postHandlerImpl                                            = new PostHandlerImpl(alarmApi, commandApi, eventApi)
  private val routeFactory: RoutesFactory[PostRequest, WebsocketRequest] = new RoutesFactory(postHandlerImpl, null)
  private val route                                                      = routeFactory.route

  "PostHandlerImpl" must {
    "handle submit command and return started command response | ESW-216" in {
      val componentName           = "test"
      val runId                   = Id("123")
      val componentType           = Assembly
      val command: ControlCommand = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)
      val componentId             = ComponentId(componentName, componentType)
      val submitRequest           = CommandRequest(componentId, command, Submit)

      when(componentFactory.commandService(componentName, componentType)).thenReturn(Future.successful(commandService))
      when(commandService.submit(command)).thenReturn(Future.successful(Started(runId)))

      Post("/post", submitRequest) ~> route ~> check {
        responseAs[Either[InvalidComponent, CommandResponse]].rightValue shouldEqual Started(runId)
      }
    }

    "handle validate command and return accepted command response | ESW-216" in {
      val componentName           = "test"
      val runId                   = Id("123")
      val componentType           = Assembly
      val command: ControlCommand = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)
      val componentId             = ComponentId(componentName, componentType)
      val validateRequest         = CommandRequest(componentId, command, Validate)

      when(componentFactory.commandService(componentName, componentType)).thenReturn(Future.successful(commandService))
      when(commandService.validate(command)).thenReturn(Future.successful(Accepted(runId)))

      Post("/post", validateRequest) ~> route ~> check {
        responseAs[Either[InvalidComponent, CommandResponse]].rightValue shouldEqual Accepted(runId)
      }
    }

    "handle oneway command and return accepted command response | ESW-216" in {
      val componentName           = "test"
      val runId                   = Id("123")
      val componentType           = Assembly
      val command: ControlCommand = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)
      val componentId             = ComponentId(componentName, componentType)
      val onewayRequest           = CommandRequest(componentId, command, Oneway)

      when(componentFactory.commandService(componentName, componentType)).thenReturn(Future.successful(commandService))
      when(commandService.oneway(command)).thenReturn(Future.successful(Accepted(runId)))

      Post("/post", onewayRequest) ~> route ~> check {
        responseAs[Either[InvalidComponent, CommandResponse]].rightValue shouldEqual Accepted(runId)
      }
    }

    "return InvalidComponent response for invalid component id | ESW-216" in {
      val componentName           = "test"
      val runId                   = Id("123")
      val componentType           = Assembly
      val command: ControlCommand = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)
      val componentId             = ComponentId(componentName, componentType)
      val submitRequest           = CommandRequest(componentId, command, Submit)

      val errmsg = s"Could not find component $componentName of type - $componentType"

      when(componentFactory.commandService(componentName, componentType))
        .thenReturn(Future.failed(new IllegalArgumentException(errmsg)))

      Post("/post", submitRequest) ~> route ~> check {
        responseAs[Either[InvalidComponent, CommandResponse]].leftValue shouldEqual InvalidComponent(errmsg)
      }
    }

    "publish an event and return Done | ESW-216" in {
      val prefix       = Prefix("tcs.test.gateway")
      val name         = EventName("event1")
      val event        = SystemEvent(prefix, name, Set.empty)
      val publishEvent = PublishEvent(event)

      when(eventPublisher.publish(event)).thenReturn(Future.successful(Done))

      Post("/post", publishEvent) ~> route ~> check {
        responseAs[Done] shouldEqual Done
      }
    }

    "get an event successfully | ESW-216" in {
      val prefix   = Prefix("tcs.test.gateway")
      val name     = EventName("event1")
      val event    = SystemEvent(prefix, name, Set.empty)
      val eventKey = EventKey(prefix, name)
      val getEvent = GetEvent(Set(eventKey))

      when(eventSubscriber.get(Set(eventKey))).thenReturn(Future.successful(Set(event)))

      Post("/post", getEvent) ~> route ~> check {
        responseAs[Either[EmptyEventKeys, Set[Event]]].rightValue shouldEqual Set(event)
      }
    }

    "get event return EmptyEventKeys error on sending no event keys in request | ESW-216" in {
      Post("/post", GetEvent(Set())) ~> route ~> check {
        responseAs[Either[EmptyEventKeys, Set[Event]]].leftValue shouldEqual EmptyEventKeys()
      }
    }

    "set alarm severity returns Done | ESW-216" in {
      val componentName    = "testComponent"
      val alarmName        = "testAlarmName"
      val subsystemName    = Subsystem.IRIS
      val majorSeverity    = AlarmSeverity.Major
      val alarmKey         = AlarmKey(subsystemName, componentName, alarmName)
      val setAlarmSeverity = SetAlarmSeverity(alarmKey, majorSeverity)

      when(alarmService.setSeverity(alarmKey, majorSeverity)).thenReturn(Future.successful(Done))

      Post("/post", setAlarmSeverity) ~> route ~> check {
        responseAs[Either[SetAlarmSeverityFailure, Done]].rightValue shouldEqual Done
      }
    }

    "set alarm severity returns SetAlarmSeverityFailure on key not found or invalid key | ESW-216" in {
      val componentName    = "testComponent"
      val alarmName        = "testAlarmName"
      val subsystemName    = Subsystem.IRIS
      val majorSeverity    = AlarmSeverity.Major
      val alarmKey         = AlarmKey(subsystemName, componentName, alarmName)
      val setAlarmSeverity = SetAlarmSeverity(alarmKey, majorSeverity)

      when(alarmService.setSeverity(alarmKey, majorSeverity)).thenReturn(Future.failed(new KeyNotFoundException("")))

      Post("/post", setAlarmSeverity) ~> route ~> check {
        responseAs[Either[SetAlarmSeverityFailure, Done]].leftValue shouldEqual SetAlarmSeverityFailure("")
      }
    }

  }

}
