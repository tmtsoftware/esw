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
import csw.logging.macros.SourceFactory
import csw.logging.models.{AnyId, Level}
import csw.params.commands.CommandResponse.{Accepted, Started}
import csw.params.commands.{CommandName, CommandResponse, Setup}
import csw.params.core.models.{Id, ObsId, Prefix, Subsystem}
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.PostRequest._
import esw.gateway.api.protocol.{EmptyEventKeys, EventServerUnavailable, InvalidComponent, SetAlarmSeverityFailure}
import esw.gateway.api.{AlarmApi, CommandApi, EventApi, LoggingApi}
import esw.gateway.impl._
import esw.gateway.server.handlers.PostHandlerImpl
import esw.http.core.BaseTestSuite
import msocket.impl.{Encoding, RouteFactory}
import msocket.impl.Encoding.JsonText
import msocket.impl.post.{ClientHttpCodecs, PostRouteFactory}
import org.mockito.ArgumentMatchers.{any, eq => argsEq}
import org.mockito.Mockito.when
import org.mockito.MockitoSugar._

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class PostRouteTest extends BaseTestSuite with ScalatestRouteTest with GatewayCodecs with ClientHttpCodecs {

  override def encoding: Encoding[_] = JsonText

  private val cswCtxMocks = new CswWiringMocks()
  import cswCtxMocks._

  implicit val timeout: Timeout = Timeout(5.seconds)

  private val alarmApi: AlarmApi     = new AlarmImpl(alarmService)
  private val eventApi: EventApi     = new EventImpl(eventService, eventSubscriberUtil)
  private val commandApi: CommandApi = new CommandImpl(commandServiceFactory)
  private val loggingApi: LoggingApi = new LoggingImpl(loggerCache)
  private val postHandlerImpl        = new PostHandlerImpl(alarmApi, commandApi, eventApi, loggingApi)
  private val route                  = RouteFactory.combine(new PostRouteFactory("post-endpoint", postHandlerImpl))

  "Submit Command" must {
    "handle submit command and return started command response | ESW-91, ESW-216" in {
      val componentName = "test"
      val runId         = Id("123")
      val componentType = Assembly
      val command       = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId")))
      val componentId   = ComponentId(componentName, componentType)
      val submitRequest = Submit(componentId, command)

      when(commandServiceFactory.commandService(componentId)).thenReturn(Future.successful(Right(commandService)))
      when(commandService.submit(command)).thenReturn(Future.successful(Started(runId)))

      Post("/post-endpoint", submitRequest) ~> route ~> check {
        responseAs[Either[InvalidComponent, CommandResponse]].rightValue shouldEqual Started(runId)
      }
    }

    "handle validate command and return accepted command response | ESW-91, ESW-216" in {
      val componentName   = "test"
      val runId           = Id("123")
      val componentType   = Assembly
      val command         = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId")))
      val componentId     = ComponentId(componentName, componentType)
      val validateRequest = Validate(componentId, command)

      when(commandServiceFactory.commandService(componentId)).thenReturn(Future.successful(Right(commandService)))
      when(commandService.validate(command)).thenReturn(Future.successful(Accepted(runId)))

      Post("/post-endpoint", validateRequest) ~> route ~> check {
        responseAs[Either[InvalidComponent, CommandResponse]].rightValue shouldEqual Accepted(runId)
      }
    }

    "handle oneway command and return accepted command response | ESW-91, ESW-216" in {
      val componentName = "test"
      val runId         = Id("123")
      val componentType = Assembly
      val command       = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId")))
      val componentId   = ComponentId(componentName, componentType)
      val onewayRequest = Oneway(componentId, command)

      when(commandServiceFactory.commandService(componentId)).thenReturn(Future.successful(Right(commandService)))
      when(commandService.oneway(command)).thenReturn(Future.successful(Accepted(runId)))

      Post("/post-endpoint", onewayRequest) ~> route ~> check {
        responseAs[Either[InvalidComponent, CommandResponse]].rightValue shouldEqual Accepted(runId)
      }
    }

    "return InvalidComponent response for invalid component id | ESW-91, ESW-216" in {
      val componentName = "test"
      val componentType = Assembly
      val command       = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId")))
      val componentId   = ComponentId(componentName, componentType)
      val submitRequest = Submit(componentId, command)

      val errmsg = s"Could not find component $componentName of type - $componentType"

      when(commandServiceFactory.commandService(componentId))
        .thenReturn(Future.successful(Left(InvalidComponent(errmsg))))

      Post("/post-endpoint", submitRequest) ~> route ~> check {
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

      Post("/post-endpoint", publishEvent) ~> route ~> check {
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

      Post("/post-endpoint", publishEvent) ~> route ~> check {
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

      Post("/post-endpoint", getEvent) ~> route ~> check {
        responseAs[Either[EmptyEventKeys.type, Set[Event]]].rightValue shouldEqual Set(event)
      }
    }

    "return EmptyEventKeys error on sending no event keys in request | ESW-94, ESW-216" in {
      Post("/post-endpoint", GetEvent(Set())) ~> route ~> check {
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

      Post("/post-endpoint", getEvent) ~> route ~> check {
        responseAs[Either[EmptyEventKeys.type, Set[Event]]].leftValue shouldEqual EventServerUnavailable
      }
    }

    "return InternalServerError if get event fails for some unwanted reason | ESW-94, ESW-216" in {
      when(eventSubscriber.get(any[Set[EventKey]])).thenReturn(Future.failed(new RuntimeException("failed")))

      val eventKey = EventKey(Prefix("tcs.test.gateway"), EventName("event1"))

      Post("/post-endpoint", GetEvent(Set(eventKey))) ~> route ~> check {
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

      Post("/post-endpoint", setAlarmSeverity) ~> route ~> check {
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

      Post("/post-endpoint", setAlarmSeverity) ~> route ~> check {
        responseAs[Either[SetAlarmSeverityFailure, Done]].leftValue shouldEqual SetAlarmSeverityFailure("")
      }
    }
  }

  "Log" must {
    "log the message, metadata and return Done | ESW-200" in {
      val log = Log(
        "esw-test",
        Level.FATAL,
        "test-message",
        Map(
          "additional-info" -> 45,
          "city"            -> "LA"
        )
      )

      Post("/post-endpoint", log) ~> route ~> check {
        responseAs[Done] shouldEqual Done
        val expectedMetadata = Map(
          "additional-info" -> 45,
          "city"            -> "LA"
        )
        verify(logger).fatal(argsEq("test-message"), argsEq(expectedMetadata), any[Throwable], any[AnyId])(
          any[SourceFactory]
        )
      }
    }

    "log the message and return Done | ESW-200" in {
      val log = Log(
        "esw-test",
        Level.FATAL,
        "test-message"
      )

      Post("/post-endpoint", log) ~> route ~> check {
        responseAs[Done] shouldEqual Done
        verify(logger).fatal(argsEq("test-message"), argsEq(Map.empty), any[Throwable], any[AnyId])(
          any[SourceFactory]
        )
      }
    }
  }
}
