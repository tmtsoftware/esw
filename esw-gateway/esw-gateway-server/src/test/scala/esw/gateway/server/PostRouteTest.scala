package esw.gateway.server

import akka.Done
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.admin.api.UnresolvedAkkaLocationException
import csw.alarm.api.exceptions.KeyNotFoundException
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.command.api.messages.CommandServiceHttpMessage.{Oneway, Submit, Validate}
import csw.event.api.exceptions.{EventServerNotAvailable, PublishFailure}
import csw.location.api.models.ComponentType.{Assembly, Sequencer}
import csw.location.api.models.{ComponentId, ComponentType}
import csw.logging.macros.SourceFactory
import csw.logging.models.{AnyId, Level, LogMetadata}
import csw.params.commands.CommandIssue.IdNotAvailableIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, ObsId}
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.prefix.models.Subsystem.IRIS
import csw.prefix.models.{Prefix, Subsystem}
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.PostRequest._
import esw.gateway.api.protocol._
import esw.gateway.server.handlers.PostHandlerImpl
import esw.http.core.BaseTestSuite
import esw.ocs.api.protocol.{Ok, OkOrUnhandledResponse, SequencerPostRequest}
import msocket.api.ContentType
import msocket.api.models.{GenericError, ServiceError}
import msocket.impl.post.{ClientHttpCodecs, PostRouteFactory}
import org.mockito.ArgumentMatchers.{any, eq => argsEq}
import org.mockito.MockitoSugar

import scala.concurrent.Future

class PostRouteTest extends BaseTestSuite with ScalatestRouteTest with GatewayCodecs with ClientHttpCodecs with MockitoSugar {

  override def clientContentType: ContentType = ContentType.Json

  private val cswCtxMocks = new CswWiringMocks()
  import cswCtxMocks._

  private val postHandlerImpl = new PostHandlerImpl(alarmApi, resolver, eventApi, loggingApi, adminService)
  private val route           = new PostRouteFactory("post-endpoint", postHandlerImpl).make()
  private val source          = Prefix("esw.test")
  private val destination     = Prefix("tcs.test")

  private def post[E: ToEntityMarshaller](entity: E): HttpRequest = Post("/post-endpoint", entity)

  "Submit Command" must {
    "handle submit command and return started command response | ESW-91, ESW-216" in {
      val runId                      = Id("123")
      val componentType              = Assembly
      val command                    = Setup(source, CommandName("c1"), Some(ObsId("obsId")))
      val componentId                = ComponentId(destination, componentType)
      val submitRequest: PostRequest = ComponentCommand(componentId, Submit(command))

      when(resolver.commandService(componentId)).thenReturn(Future.successful(commandService))
      when(commandService.submit(command)).thenReturn(Future.successful(Started(runId)))

      post(submitRequest) ~> route ~> check {
        responseAs[SubmitResponse] shouldEqual Started(runId)
      }
    }

    "handle validate command and return accepted command response | ESW-91, ESW-216" in {
      val runId                        = Id("123")
      val componentType                = Assembly
      val command                      = Setup(source, CommandName("c1"), Some(ObsId("obsId")))
      val componentId                  = ComponentId(destination, componentType)
      val validateRequest: PostRequest = ComponentCommand(componentId, Validate(command))

      when(resolver.commandService(componentId)).thenReturn(Future.successful(commandService))
      when(commandService.validate(command)).thenReturn(Future.successful(Accepted(runId)))

      post(validateRequest) ~> route ~> check {
        responseAs[ValidateResponse] shouldEqual Accepted(runId)
      }
    }

    "handle oneway command and return accepted command response | ESW-91, ESW-216" in {
      val runId                      = Id("123")
      val componentType              = Assembly
      val command                    = Setup(source, CommandName("c1"), Some(ObsId("obsId")))
      val componentId                = ComponentId(destination, componentType)
      val onewayRequest: PostRequest = ComponentCommand(componentId, Oneway(command))

      when(resolver.commandService(componentId)).thenReturn(Future.successful(commandService))
      when(commandService.oneway(command)).thenReturn(Future.successful(Accepted(runId)))

      Post("/post-endpoint", onewayRequest) ~> route ~> check {
        responseAs[ValidateResponse] shouldEqual Accepted(runId)
      }
    }

    "return InvalidComponent response for invalid component id | ESW-91, ESW-216" in {
      val componentType              = Assembly
      val command                    = Setup(source, CommandName("c1"), Some(ObsId("obsId")))
      val componentId                = ComponentId(destination, componentType)
      val submitRequest: PostRequest = ComponentCommand(componentId, Submit(command))

      val message = "component does not exist"
      when(resolver.commandService(componentId)).thenReturn(Future.failed(InvalidComponent(message)))

      post(submitRequest) ~> route ~> check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[GatewayException] shouldEqual InvalidComponent(message)
      }
    }
  }

  "SequencerRoutes" must {
    "handle submit command and return started command response | ESW-250" in {
      val sequence                   = Sequence(Setup(source, CommandName("c1"), Some(ObsId("obsId"))))
      val componentId                = ComponentId(destination, Sequencer)
      val submitRequest: PostRequest = SequencerCommand(componentId, SequencerPostRequest.Submit(sequence))
      val submitResponse             = Started(Id("123"))

      when(resolver.sequencerCommandService(componentId)).thenReturn(Future.successful(sequencer))
      when(sequencer.submit(sequence)).thenReturn(Future.successful(submitResponse))

      post(submitRequest) ~> route ~> check {
        responseAs[SubmitResponse] shouldEqual submitResponse
      }
    }

    "handle query command and return query response | ESW-250" in {
      val runId                     = Id("runId")
      val componentId               = ComponentId(destination, Sequencer)
      val queryRequest: PostRequest = SequencerCommand(componentId, SequencerPostRequest.Query(runId))
      val queryResponse             = Invalid(runId, IdNotAvailableIssue(s"Sequencer is not running any sequence with runId $runId"))

      when(resolver.sequencerCommandService(componentId)).thenReturn(Future.successful(sequencer))
      when(sequencer.query(runId)).thenReturn(Future.successful(queryResponse))

      post(queryRequest) ~> route ~> check {
        responseAs[SubmitResponse] shouldEqual queryResponse
      }
    }

    "handle go online command and return Ok response | ESW-250" in {
      val componentId                  = ComponentId(destination, Sequencer)
      val goOnlineRequest: PostRequest = SequencerCommand(componentId, SequencerPostRequest.GoOnline)

      when(resolver.sequencerCommandService(componentId)).thenReturn(Future.successful(sequencer))
      when(sequencer.goOnline()).thenReturn(Future.successful(Ok))

      post(goOnlineRequest) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] shouldEqual Ok
      }
    }
  }

  "Publish Event" must {
    "return Done on successful publish | ESW-92, ESW-216" in {
      val prefix                    = Prefix("tcs.test.gateway")
      val name                      = EventName("event1")
      val event                     = SystemEvent(prefix, name, Set.empty)
      val publishEvent: PostRequest = PublishEvent(event)

      when(eventPublisher.publish(event)).thenReturn(Future.successful(Done))

      post(publishEvent) ~> route ~> check {
        responseAs[Done] shouldEqual Done
      }
    }

    "return EventServerUnavailable error when EventServer is down | ESW-92, ESW-216" in {
      val prefix                    = Prefix("tcs.test.gateway")
      val name                      = EventName("event1")
      val event                     = SystemEvent(prefix, name, Set.empty)
      val publishEvent: PostRequest = PublishEvent(event)

      when(eventPublisher.publish(event))
        .thenReturn(Future.failed(PublishFailure(event, new RuntimeException("Event server is down"))))

      post(publishEvent) ~> route ~> check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[GatewayException] shouldEqual EventServerUnavailable()
      }
    }
  }

  "Get Event" must {
    "return an event successfully | ESW-94, ESW-216" in {
      val prefix                = Prefix("tcs.test.gateway")
      val name                  = EventName("event1")
      val event                 = SystemEvent(prefix, name, Set.empty)
      val eventKey              = EventKey(prefix, name)
      val getEvent: PostRequest = GetEvent(Set(eventKey))

      when(eventSubscriber.get(Set(eventKey))).thenReturn(Future.successful(Set(event)))

      post(getEvent) ~> route ~> check {
        responseAs[Set[Event]] shouldEqual Set(event)
      }
    }

    "return EmptyEventKeys error on sending no event keys in request | ESW-94, ESW-216" in {
      post(GetEvent(Set()): PostRequest) ~> route ~> check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[GatewayException] shouldEqual EmptyEventKeys()
      }
    }

    "return EventServerUnavailable error when EventServer is down | ESW-94, ESW-216" in {
      val prefix                = Prefix("tcs.test.gateway")
      val name                  = EventName("event1")
      val eventKey              = EventKey(prefix, name)
      val getEvent: PostRequest = GetEvent(Set(eventKey))

      when(eventSubscriber.get(Set(eventKey)))
        .thenReturn(Future.failed(EventServerNotAvailable(new RuntimeException("Redis server is not available"))))

      post(getEvent) ~> route ~> check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[GatewayException] shouldEqual EventServerUnavailable()
      }
    }

    "handle exceptions if get event fails for some unwanted reason | ESW-94, ESW-216" in {
      when(eventSubscriber.get(any[Set[EventKey]])).thenReturn(Future.failed(new RuntimeException("failed")))

      val eventKey = EventKey(Prefix("tcs.test.gateway"), EventName("event1"))

      post(GetEvent(Set(eventKey)): PostRequest) ~> route ~> check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[ServiceError] shouldEqual ServiceError.fromThrowable(new RuntimeException("failed"))
      }
    }
  }

  "Set Alarm Severity" must {
    "return Done on success | ESW-193, ESW-216, ESW-233, CSW-83" in {
      val alarmName                     = "testAlarmName"
      val majorSeverity                 = AlarmSeverity.Major
      val alarmKey                      = AlarmKey(Prefix(IRIS, "test_component"), alarmName)
      val setAlarmSeverity: PostRequest = SetAlarmSeverity(alarmKey, majorSeverity)

      when(alarmService.setSeverity(alarmKey, majorSeverity)).thenReturn(Future.successful(Done))

      post(setAlarmSeverity) ~> route ~> check {
        responseAs[Done] shouldEqual Done
      }
    }

    "return SetAlarmSeverityFailure on key not found or invalid key | ESW-193, ESW-216, ESW-233, CSW-83" in {
      val alarmName                     = "testAlarmName"
      val majorSeverity                 = AlarmSeverity.Major
      val alarmKey                      = AlarmKey(Prefix(IRIS, "test_component"), alarmName)
      val setAlarmSeverity: PostRequest = SetAlarmSeverity(alarmKey, majorSeverity)

      when(alarmService.setSeverity(alarmKey, majorSeverity)).thenReturn(Future.failed(new KeyNotFoundException("")))

      post(setAlarmSeverity) ~> route ~> check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[GatewayException] shouldEqual SetAlarmSeverityFailure("")
      }
    }
  }

  "Log" must {
    "log the message, metadata and return Done | ESW-200, CSW-63, CSW-78" in {
      val log: PostRequest = Log(
        Prefix("esw.test"),
        Level.FATAL,
        "test-message",
        Map(
          "additional-info" -> 45,
          "city"            -> "LA"
        )
      )

      post(log) ~> route ~> check {
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

    "log the message and return Done | ESW-200, CSW-63, CSW-78" in {
      val log: PostRequest = Log(
        Prefix("esw.test"),
        Level.FATAL,
        "test-message"
      )

      post(log) ~> route ~> check {
        responseAs[Done] shouldEqual Done
        verify(logger).fatal(argsEq("test-message"), argsEq(Map.empty), any[Throwable], any[AnyId])(
          any[SourceFactory]
        )
      }
    }
  }

  "GetLogMetadata" must {
    "return log metadata for given component | ESW-254" in {
      val componentId = ComponentId(Prefix(Subsystem.ESW, "test1"), ComponentType.Assembly)
      val metadata    = LogMetadata(Level.FATAL, Level.FATAL, Level.FATAL, Level.FATAL)

      when(adminService.getLogMetadata(componentId)).thenReturn(Future.successful(metadata))

      post(GetLogMetadata(componentId): PostRequest) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[LogMetadata] shouldEqual metadata
      }
    }

    "return generic error when component is not resolved | ESW-254, ESW-279" in {
      val componentId = ComponentId(Prefix(Subsystem.ESW, "test1"), ComponentType.Assembly)
      val error       = GenericError("UnresolvedAkkaLocationException", "Could not resolve ESW.test1 to a valid Akka location")

      when(adminService.getLogMetadata(componentId))
        .thenReturn(Future.failed(new UnresolvedAkkaLocationException(componentId.prefix)))

      post(GetLogMetadata(componentId): PostRequest) ~> route ~> check {
        responseAs[GenericError] shouldEqual error
      }
    }
  }

  "SetLogMetadata" must {
    "set log level for given component | ESW-254" in {
      val componentId = ComponentId(Prefix(Subsystem.ESW, "test1"), ComponentType.Assembly)

      when(adminService.setLogLevel(componentId, Level.FATAL)).thenReturn(Future.unit)

      post(SetLogLevel(componentId, Level.FATAL): PostRequest) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Unit] shouldEqual ()
      }
    }

    "return generic error when component is not resolved | ESW-254, ESW-279" in {
      val componentId = ComponentId(Prefix(Subsystem.ESW, "test1"), ComponentType.Assembly)
      val error       = GenericError("UnresolvedAkkaLocationException", "Could not resolve ESW.test1 to a valid Akka location")

      when(adminService.setLogLevel(componentId, Level.FATAL))
        .thenReturn(Future.failed(new UnresolvedAkkaLocationException(componentId.prefix)))

      post(SetLogLevel(componentId, Level.FATAL): PostRequest) ~> route ~> check {
        responseAs[GenericError] shouldEqual error
      }
    }
  }
}
