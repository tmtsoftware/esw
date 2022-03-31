/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.gateway.server

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.http.SecurityDirectives
import csw.alarm.api.exceptions.KeyNotFoundException
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.command.api.messages.CommandServiceRequest.{Oneway, Submit, Validate}
import csw.command.client.auth.CommandRoles
import csw.command.client.models.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.event.api.exceptions.{EventServerNotAvailable, PublishFailure}
import csw.location.api.models.ComponentType.{Assembly, Sequencer}
import csw.location.api.models.{ComponentId, ComponentType}
import csw.logging.macros.SourceFactory
import csw.logging.models.{Level, LogMetadata}
import csw.params.commands.CommandIssue.IdNotAvailableIssue
import csw.params.commands.CommandResponse.*
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, ObsId}
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.prefix.models.Subsystem.IRIS
import csw.prefix.models.{Prefix, Subsystem}
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.*
import esw.gateway.api.protocol.GatewayRequest.*
import esw.gateway.server.handlers.GatewayPostHandler
import esw.ocs.api.protocol.{Ok, OkOrUnhandledResponse, SequencerRequest}
import esw.testcommons.BaseTestSuite
import msocket.api.ContentType
import msocket.api.models.ServiceError
import msocket.http.post.{ClientHttpCodecs, PostRouteFactory}
import msocket.jvm.metrics.LabelExtractor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}

import scala.concurrent.Future

class GatewayPostRouteTest extends BaseTestSuite with ScalatestRouteTest with GatewayCodecs with ClientHttpCodecs {

  override def clientContentType: ContentType = ContentType.Json
  implicit val typedSystem: ActorSystem[_]    = system.toTyped
  private val cswCtxMocks                     = new CswTestMocks()
  import cswCtxMocks.*

  private val securityDirectives = SecurityDirectives.authDisabled(system.settings.config)
  private val commandRoles       = CommandRoles.empty

  private val postHandlerImpl =
    new GatewayPostHandler(alarmApi, resolver, eventApi, loggingApi, adminApi, securityDirectives, commandRoles)

  import LabelExtractor.Implicits.default
  private val route = new PostRouteFactory("post-endpoint", postHandlerImpl).make()

  private val source      = Prefix("esw.test")
  private val destination = Prefix("tcs.test")

  private def post[E: ToEntityMarshaller](entity: E): HttpRequest = Post("/post-endpoint", entity)

  override protected def afterEach(): Unit = {
    reset(logger)
  }

  "Submit Command" must {
    "handle submit command and return started command response | ESW-91, ESW-216" in {
      val runId                         = Id("123")
      val componentType                 = Assembly
      val command                       = Setup(source, CommandName("c1"), Some(ObsId("2020A-001-123")))
      val componentId                   = ComponentId(destination, componentType)
      val submitRequest: GatewayRequest = ComponentCommand(componentId, Submit(command))

      when(resolver.commandService(componentId)).thenReturn(Future.successful(commandService))
      when(commandService.submit(command)).thenReturn(Future.successful(Started(runId)))

      post(submitRequest) ~> route ~> check {
        responseAs[SubmitResponse] shouldEqual Started(runId)
      }
    }

    "handle validate command and return accepted command response | ESW-91, ESW-216" in {
      val runId                           = Id("123")
      val componentType                   = Assembly
      val command                         = Setup(source, CommandName("c1"), Some(ObsId("2020A-001-123")))
      val componentId                     = ComponentId(destination, componentType)
      val validateRequest: GatewayRequest = ComponentCommand(componentId, Validate(command))

      when(resolver.commandService(componentId)).thenReturn(Future.successful(commandService))
      when(commandService.validate(command)).thenReturn(Future.successful(Accepted(runId)))

      post(validateRequest) ~> route ~> check {
        responseAs[ValidateResponse] shouldEqual Accepted(runId)
      }
    }

    "handle oneway command and return accepted command response | ESW-91, ESW-216" in {
      val runId                         = Id("123")
      val componentType                 = Assembly
      val command                       = Setup(source, CommandName("c1"), Some(ObsId("2020A-001-123")))
      val componentId                   = ComponentId(destination, componentType)
      val onewayRequest: GatewayRequest = ComponentCommand(componentId, Oneway(command))

      when(resolver.commandService(componentId)).thenReturn(Future.successful(commandService))
      when(commandService.oneway(command)).thenReturn(Future.successful(Accepted(runId)))

      Post("/post-endpoint", onewayRequest) ~> route ~> check {
        responseAs[ValidateResponse] shouldEqual Accepted(runId)
      }
    }

    "return InvalidComponent response for invalid component id | ESW-91, ESW-216" in {
      val componentType                 = Assembly
      val command                       = Setup(source, CommandName("c1"), Some(ObsId("2020A-001-123")))
      val componentId                   = ComponentId(destination, componentType)
      val submitRequest: GatewayRequest = ComponentCommand(componentId, Submit(command))

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
      val sequence                      = Sequence(Setup(source, CommandName("c1"), Some(ObsId("2020A-001-123"))))
      val componentId                   = ComponentId(destination, Sequencer)
      val submitRequest: GatewayRequest = SequencerCommand(componentId, SequencerRequest.Submit(sequence))
      val submitResponse                = Started(Id("123"))

      when(resolver.sequencerCommandService(componentId)).thenReturn(Future.successful(sequencer))
      when(sequencer.submit(sequence)).thenReturn(Future.successful(submitResponse))

      post(submitRequest) ~> route ~> check {
        responseAs[SubmitResponse] shouldEqual submitResponse
      }
    }

    "handle query command and return query response | ESW-250" in {
      val runId                        = Id("runId")
      val componentId                  = ComponentId(destination, Sequencer)
      val queryRequest: GatewayRequest = SequencerCommand(componentId, SequencerRequest.Query(runId))
      val queryResponse = Invalid(runId, IdNotAvailableIssue(s"Sequencer is not running any sequence with runId $runId"))

      when(resolver.sequencerCommandService(componentId)).thenReturn(Future.successful(sequencer))
      when(sequencer.query(runId)).thenReturn(Future.successful(queryResponse))

      post(queryRequest) ~> route ~> check {
        responseAs[SubmitResponse] shouldEqual queryResponse
      }
    }

    "handle go online command and return Ok response | ESW-250" in {
      val componentId                     = ComponentId(destination, Sequencer)
      val goOnlineRequest: GatewayRequest = SequencerCommand(componentId, SequencerRequest.GoOnline)

      when(resolver.sequencerCommandService(componentId)).thenReturn(Future.successful(sequencer))
      when(sequencer.goOnline()).thenReturn(Future.successful(Ok))

      post(goOnlineRequest) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] shouldEqual Ok
      }
    }
  }

  "Publish Event" must {
    "return Done on successful publish | ESW-92, ESW-216" in {
      val prefix                       = Prefix("tcs.test.gateway")
      val name                         = EventName("event1")
      val event                        = SystemEvent(prefix, name, Set.empty)
      val publishEvent: GatewayRequest = PublishEvent(event)

      when(eventPublisher.publish(event)).thenReturn(Future.successful(Done))

      post(publishEvent) ~> route ~> check {
        responseAs[Done] shouldEqual Done
      }
    }

    "return EventServerUnavailable error when EventServer is down | ESW-92, ESW-216" in {
      val prefix                       = Prefix("tcs.test.gateway")
      val name                         = EventName("event1")
      val event                        = SystemEvent(prefix, name, Set.empty)
      val publishEvent: GatewayRequest = PublishEvent(event)

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
      val prefix                   = Prefix("tcs.test.gateway")
      val name                     = EventName("event1")
      val event                    = SystemEvent(prefix, name, Set.empty)
      val eventKey                 = EventKey(prefix, name)
      val getEvent: GatewayRequest = GetEvent(Set(eventKey))

      when(eventSubscriber.get(Set(eventKey))).thenReturn(Future.successful(Set(event)))

      post(getEvent) ~> route ~> check {
        responseAs[Set[Event]] shouldEqual Set(event)
      }
    }

    "return EmptyEventKeys error on sending no event keys in request | ESW-94, ESW-216" in {
      post(GetEvent(Set()): GatewayRequest) ~> route ~> check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[GatewayException] shouldEqual EmptyEventKeys()
      }
    }

    "return EventServerUnavailable error when EventServer is down | ESW-94, ESW-216" in {
      val prefix                   = Prefix("tcs.test.gateway")
      val name                     = EventName("event1")
      val eventKey                 = EventKey(prefix, name)
      val getEvent: GatewayRequest = GetEvent(Set(eventKey))

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

      post(GetEvent(Set(eventKey)): GatewayRequest) ~> route ~> check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[ServiceError] shouldEqual ServiceError.fromThrowable(new RuntimeException("failed"))
      }
    }
  }

  "Set Alarm Severity" must {
    "return Done on success | ESW-193, ESW-216, ESW-233, CSW-83" in {
      val alarmName                        = "testAlarmName"
      val majorSeverity                    = AlarmSeverity.Major
      val alarmKey                         = AlarmKey(Prefix(IRIS, "test_component"), alarmName)
      val setAlarmSeverity: GatewayRequest = SetAlarmSeverity(alarmKey, majorSeverity)

      when(alarmService.setSeverity(alarmKey, majorSeverity)).thenReturn(Future.successful(Done))

      post(setAlarmSeverity) ~> route ~> check {
        responseAs[Done] shouldEqual Done
      }
    }

    "return SetAlarmSeverityFailure on key not found or invalid key | ESW-193, ESW-216, ESW-233, CSW-83" in {
      val alarmName                        = "testAlarmName"
      val majorSeverity                    = AlarmSeverity.Major
      val alarmKey                         = AlarmKey(Prefix(IRIS, "test_component"), alarmName)
      val setAlarmSeverity: GatewayRequest = SetAlarmSeverity(alarmKey, majorSeverity)

      when(alarmService.setSeverity(alarmKey, majorSeverity)).thenReturn(Future.failed(new KeyNotFoundException("")))

      post(setAlarmSeverity) ~> route ~> check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[GatewayException] shouldEqual SetAlarmSeverityFailure("")
      }
    }
  }

  "Log" must {

    "log the message, metadata and return Done | ESW-200, CSW-63, CSW-78" in {
      val log: GatewayRequest = Log(
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

        verify(logger).fatal("test-message", expectedMetadata)(SourceFactory.factory)
      }
    }

    "log the message and return Done | ESW-200, CSW-63, CSW-78" in {
      val log: GatewayRequest = Log(
        Prefix("esw.test"),
        Level.FATAL,
        "test-message"
      )

      post(log) ~> route ~> check {
        responseAs[Done] shouldEqual Done
        verify(logger).fatal("test-message", Map.empty)(SourceFactory.factory)
      }
    }
  }

  "GetLogMetadata" must {
    "return log metadata for given component | ESW-254" in {
      val componentId = ComponentId(Prefix(Subsystem.ESW, "test1"), ComponentType.Assembly)
      val metadata    = LogMetadata(Level.FATAL, Level.FATAL, Level.FATAL, Level.FATAL)

      when(adminApi.getLogMetadata(componentId)).thenReturn(Future.successful(metadata))

      post(GetLogMetadata(componentId): GatewayRequest) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[LogMetadata] shouldEqual metadata
      }
    }

    "return generic error when component is not resolved | ESW-254, ESW-279" in {
      val componentId = ComponentId(Prefix(Subsystem.ESW, "test1"), ComponentType.Assembly)

      val invalidComponentException = InvalidComponent(componentId.toString)
      when(adminApi.getLogMetadata(componentId))
        .thenReturn(Future.failed(invalidComponentException))

      post(GetLogMetadata(componentId): GatewayRequest) ~> route ~> check {
        responseAs[GatewayException] shouldEqual invalidComponentException
      }
    }
  }

  "SetLogMetadata" must {
    "set log level for given component | ESW-254" in {
      val componentId = ComponentId(Prefix(Subsystem.ESW, "test1"), ComponentType.Assembly)

      when(adminApi.setLogLevel(componentId, Level.FATAL)).thenReturn(Future.successful(Done))

      post(SetLogLevel(componentId, Level.FATAL): GatewayRequest) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Done] shouldEqual Done
      }
    }

    "return generic error when component is not resolved | ESW-254, ESW-279" in {
      val componentId = ComponentId(Prefix(Subsystem.ESW, "test1"), ComponentType.Assembly)

      val invalidComponentException = InvalidComponent(componentId.toString)
      when(adminApi.setLogLevel(componentId, Level.FATAL))
        .thenReturn(Future.failed(invalidComponentException))

      post(SetLogLevel(componentId, Level.FATAL): GatewayRequest) ~> route ~> check {
        responseAs[GatewayException] shouldEqual invalidComponentException
      }
    }
  }

  "Shutdown" must {
    val componentType = randomFrom(List(ComponentType.Assembly, ComponentType.HCD, ComponentType.Container))
    val componentId   = ComponentId(Prefix(randomSubsystem, randomString(10)), componentType)

    "should call the shutdown api of adminApi with the given componentId | ESW-378" in {
      when(adminApi.shutdown(componentId)).thenReturn(Future.successful(Done))

      post(Shutdown(componentId): GatewayRequest) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Done] shouldEqual Done
      }
    }

    "return generic error when component is not resolved | ESW-378" in {
      val invalidComponentException = InvalidComponent(componentId.toString)

      when(adminApi.shutdown(componentId)).thenReturn(Future.failed(invalidComponentException))

      post(Shutdown(componentId): GatewayRequest) ~> route ~> check {
        responseAs[GatewayException] shouldEqual invalidComponentException
      }
    }
  }

  "Restart" must {
    val componentType = randomFrom(List(ComponentType.Assembly, ComponentType.HCD, ComponentType.Container))
    val componentId   = ComponentId(Prefix(randomSubsystem, randomString(10)), componentType)

    "should call the restart api of adminApi with the given componentId | ESW-378" in {
      when(adminApi.restart(componentId)).thenReturn(Future.successful(Done))

      post(Restart(componentId): GatewayRequest) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Done] shouldEqual Done
      }
    }

    "return generic error when component is not resolved | ESW-378" in {
      val invalidComponentException = InvalidComponent(componentId.toString)

      when(adminApi.restart(componentId)).thenReturn(Future.failed(invalidComponentException))

      post(Restart(componentId): GatewayRequest) ~> route ~> check {
        responseAs[GatewayException] shouldEqual invalidComponentException
      }
    }
  }

  "GoOnline" must {
    val componentType = randomFrom(List(ComponentType.Assembly, ComponentType.HCD, ComponentType.Container))
    val componentId   = ComponentId(Prefix(randomSubsystem, randomString(10)), componentType)

    "should call the goOnline api of adminApi with the given componentId | ESW-378" in {
      when(adminApi.goOnline(componentId)).thenReturn(Future.successful(Done))

      post(GoOnline(componentId): GatewayRequest) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Done] shouldEqual Done
      }
    }

    "return generic error when component is not resolved | ESW-378" in {
      val invalidComponentException = InvalidComponent(componentId.toString)

      when(adminApi.goOnline(componentId)).thenReturn(Future.failed(invalidComponentException))

      post(GoOnline(componentId): GatewayRequest) ~> route ~> check {
        responseAs[GatewayException] shouldEqual invalidComponentException
      }
    }
  }

  "GoOffline" must {
    val componentType = randomFrom(List(ComponentType.Assembly, ComponentType.HCD, ComponentType.Container))
    val componentId   = ComponentId(Prefix(randomSubsystem, randomString(10)), componentType)

    "should call the goOffline api of adminApi with the given componentId | ESW-378" in {
      when(adminApi.goOffline(componentId)).thenReturn(Future.successful(Done))

      post(GoOffline(componentId): GatewayRequest) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Done] shouldEqual Done
      }
    }

    "return generic error when component is not resolved | ESW-378" in {
      val invalidComponentException = InvalidComponent(componentId.toString)

      when(adminApi.goOffline(componentId)).thenReturn(Future.failed(invalidComponentException))

      post(GoOffline(componentId): GatewayRequest) ~> route ~> check {
        responseAs[GatewayException] shouldEqual invalidComponentException
      }
    }
  }

  "GetContainerLifecycleState" must {
    val prefix      = Prefix(randomSubsystem, randomString(10))
    val componentId = ComponentId(prefix, ComponentType.Container)

    "should call the goOffline api of adminApi with the given componentId | ESW-378" in {
      val lifecycleState = randomFrom(ContainerLifecycleState.values.toList)

      when(adminApi.getContainerLifecycleState(prefix)).thenReturn(Future.successful(lifecycleState))

      post(GetContainerLifecycleState(prefix): GatewayRequest) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[ContainerLifecycleState] shouldEqual lifecycleState
      }
    }

    "return generic error when component is not resolved | ESW-378" in {
      val invalidComponentException = InvalidComponent(componentId.toString)

      when(adminApi.getContainerLifecycleState(prefix)).thenReturn(Future.failed(invalidComponentException))

      post(GetContainerLifecycleState(prefix): GatewayRequest) ~> route ~> check {
        responseAs[GatewayException] shouldEqual invalidComponentException
      }
    }
  }

  "GetComponentLifecycleState" must {
    val componentType = randomFrom(List(ComponentType.Assembly, ComponentType.HCD, ComponentType.Container))
    val componentId   = ComponentId(Prefix(randomSubsystem, randomString(10)), componentType)

    "should call the goOffline api of adminApi with the given componentId | ESW-378" in {
      val lifecycleState = randomFrom(SupervisorLifecycleState.values.toList)

      when(adminApi.getComponentLifecycleState(componentId)).thenReturn(Future.successful(lifecycleState))

      post(GetComponentLifecycleState(componentId): GatewayRequest) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[SupervisorLifecycleState] shouldEqual lifecycleState
      }
    }

    "return generic error when component is not resolved | ESW-378" in {
      val invalidComponentException = InvalidComponent(componentId.toString)

      when(adminApi.getComponentLifecycleState(componentId)).thenReturn(Future.failed(invalidComponentException))

      post(GetComponentLifecycleState(componentId): GatewayRequest) ~> route ~> check {
        responseAs[GatewayException] shouldEqual invalidComponentException
      }
    }
  }
}
