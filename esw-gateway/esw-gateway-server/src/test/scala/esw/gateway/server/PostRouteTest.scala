package esw.gateway.server

import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.Timeout
import csw.alarm.api.exceptions.KeyNotFoundException
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.event.api.exceptions.{EventServerNotAvailable, PublishFailure}
import csw.location.models.ComponentId
import csw.location.models.ComponentType.Assembly
import csw.params.commands.CommandResponse.{Accepted, Started}
import csw.params.commands.{CommandName, CommandResponse, Setup}
import csw.params.core.models.{Id, ObsId, Prefix, Subsystem}
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.PostRequest.{GetEvent, Oneway, PublishEvent, SetAlarmSeverity, Submit, Validate}
import esw.gateway.api.protocol.{EmptyEventKeys, EventServerUnavailable, InvalidComponent, SetAlarmSeverityFailure}
import esw.gateway.api.{AlarmApi, CommandApi, EventApi}
import esw.gateway.impl.{AlarmImpl, CommandImpl, EventImpl}
import esw.gateway.server.handlers.PostHandlerImpl
import esw.http.core.BaseTestSuite
import mscoket.impl.HttpCodecs
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.MockitoSugar._

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class PostRouteTest extends BaseTestSuite with ScalatestRouteTest with GatewayCodecs with HttpCodecs {
  val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test-system")

  protected override def afterAll(): Unit = {
    super.afterAll()
    actorSystem.terminate()
  }

  private val cswCtxMocks = new CswWiringMocks(actorSystem)
  import cswCtxMocks._

  implicit val timeout: Timeout = Timeout(5.seconds)

  private val alarmApi: AlarmApi     = new AlarmImpl(alarmService)
  private val eventApi: EventApi     = new EventImpl(eventService, eventSubscriberUtil)
  private val commandApi: CommandApi = new CommandImpl(componentFactory.commandService)
  private val postHandlerImpl        = new PostHandlerImpl(alarmApi, commandApi, eventApi)
  private val route                  = new Routes(postHandlerImpl, null, logger).route

  // fixme: add failure scenario when event server/ alarm server is down
  "Submit Command" must {
    "handle submit command and return started command response | ESW-91, ESW-216" in {
      val componentName = "test"
      val runId         = Id("123")
      val componentType = Assembly
      val command       = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)
      val componentId   = ComponentId(componentName, componentType)
      val submitRequest = Submit(componentId, command)

      when(componentFactory.commandService(componentId)).thenReturn(Future.successful(commandService))
      when(commandService.submit(command)).thenReturn(Future.successful(Started(runId)))

      Post("/post", submitRequest) ~> route ~> check {
        responseAs[Either[InvalidComponent, CommandResponse]].rightValue shouldEqual Started(runId)
      }
    }

    "handle validate command and return accepted command response | ESW-91, ESW-216" in {
      val componentName   = "test"
      val runId           = Id("123")
      val componentType   = Assembly
      val command         = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)
      val componentId     = ComponentId(componentName, componentType)
      val validateRequest = Validate(componentId, command)

      when(componentFactory.commandService(componentId)).thenReturn(Future.successful(commandService))
      when(commandService.validate(command)).thenReturn(Future.successful(Accepted(runId)))

      Post("/post", validateRequest) ~> route ~> check {
        responseAs[Either[InvalidComponent, CommandResponse]].rightValue shouldEqual Accepted(runId)
      }
    }

    "handle oneway command and return accepted command response | ESW-91, ESW-216" in {
      val componentName = "test"
      val runId         = Id("123")
      val componentType = Assembly
      val command       = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)
      val componentId   = ComponentId(componentName, componentType)
      val onewayRequest = Oneway(componentId, command)

      when(componentFactory.commandService(componentId)).thenReturn(Future.successful(commandService))
      when(commandService.oneway(command)).thenReturn(Future.successful(Accepted(runId)))

      Post("/post", onewayRequest) ~> route ~> check {
        responseAs[Either[InvalidComponent, CommandResponse]].rightValue shouldEqual Accepted(runId)
      }
    }

    "return InvalidComponent response for invalid component id | ESW-91, ESW-216" in {
      val componentName = "test"
      val runId         = Id("123")
      val componentType = Assembly
      val command       = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)
      val componentId   = ComponentId(componentName, componentType)
      val submitRequest = Submit(componentId, command)

      val errmsg = s"Could not find component $componentName of type - $componentType"

      when(componentFactory.commandService(componentId))
        .thenReturn(Future.failed(new IllegalArgumentException(errmsg)))

      Post("/post", submitRequest) ~> route ~> check {
        responseAs[Either[InvalidComponent, CommandResponse]].leftValue shouldEqual InvalidComponent(errmsg)
      }
    }
  }

  "Publish Event" must {
    "return Done on successful publish | ESW-92, ESW-216" in {
      val prefix       = Prefix("tcs.test.gateway")
      val name         = EventName("event1")
      val event        = SystemEvent(prefix, name, Set.empty)
      val publishEvent = PublishEvent(event)

      when(eventPublisher.publish(event)).thenReturn(Future.successful(Done))

      Post("/post", publishEvent) ~> route ~> check {
        responseAs[Either[EventServerUnavailable.type, Done]].rightValue shouldEqual Done
      }
    }

    "return EventServerUnavailable error when EventServer is down | ESW-92, ESW-216" in {
      val prefix       = Prefix("tcs.test.gateway")
      val name         = EventName("event1")
      val event        = SystemEvent(prefix, name, Set.empty)
      val publishEvent = PublishEvent(event)

      when(eventPublisher.publish(event))
        .thenReturn(Future.failed(PublishFailure(event, new RuntimeException("Event server is down"))))

      Post("/post", publishEvent) ~> route ~> check {
        responseAs[Either[EventServerUnavailable.type, Done]].leftValue shouldEqual EventServerUnavailable
      }
    }
  }

  "Get Event" must {
    "return an event successfully | ESW-94, ESW-216" in {
      val prefix   = Prefix("tcs.test.gateway")
      val name     = EventName("event1")
      val event    = SystemEvent(prefix, name, Set.empty)
      val eventKey = EventKey(prefix, name)
      val getEvent = GetEvent(Set(eventKey))

      when(eventSubscriber.get(Set(eventKey))).thenReturn(Future.successful(Set(event)))

      Post("/post", getEvent) ~> route ~> check {
        responseAs[Either[EmptyEventKeys.type, Set[Event]]].rightValue shouldEqual Set(event)
      }
    }

    "return EmptyEventKeys error on sending no event keys in request | ESW-94, ESW-216" in {
      Post("/post", GetEvent(Set())) ~> route ~> check {
        responseAs[Either[EmptyEventKeys.type, Set[Event]]].leftValue shouldEqual EmptyEventKeys
      }
    }

    "return EventServerUnavailable error when EventServer is down | ESW-94, ESW-216" in {
      val prefix   = Prefix("tcs.test.gateway")
      val name     = EventName("event1")
      val eventKey = EventKey(prefix, name)
      val getEvent = GetEvent(Set(eventKey))

      when(eventSubscriber.get(Set(eventKey)))
        .thenReturn(Future.failed(EventServerNotAvailable(new RuntimeException("Redis server is not available"))))

      Post("/post", getEvent) ~> route ~> check {
        responseAs[Either[EmptyEventKeys.type, Set[Event]]].leftValue shouldEqual EventServerUnavailable
      }
    }

    "return InternalServerError if get event fails for some unwanted reason | ESW-94, ESW-216" in {
      when(eventSubscriber.get(any[Set[EventKey]])).thenReturn(Future.failed(new RuntimeException("failed")))

      val eventKey = EventKey(Prefix("tcs.test.gateway"), EventName("event1"))

      Post("/post", GetEvent(Set(eventKey))) ~> route ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Set Alarm Severity" must {
    "returns Done on success | ESW-193, ESW-216" in {
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

    "returns SetAlarmSeverityFailure on key not found or invalid key | ESW-193, ESW-216" in {
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
