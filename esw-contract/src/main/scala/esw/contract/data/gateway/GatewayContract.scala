package esw.contract.data.gateway

import akka.Done
import csw.alarm.models.AlarmSeverity
import csw.command.client.models.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.contract.ResourceFetcher
import csw.contract.generator.*
import csw.contract.generator.ClassNameHelpers.*
import csw.logging.models.{Level, LogMetadata}
import csw.params.commands.CommandResponse.{SubmitResponse, ValidateResponse}
import csw.params.events.Event
import csw.prefix.models.Subsystem
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.*
import esw.gateway.api.protocol.GatewayRequest.{
  ComponentCommand,
  GetComponentLifecycleState,
  GetContainerLifecycleState,
  GetEvent,
  GetLogMetadata,
  GoOffline,
  GoOnline,
  Log,
  PublishEvent,
  Restart,
  SequencerCommand,
  SetAlarmSeverity,
  SetLogLevel,
  Shutdown
}
import esw.gateway.api.protocol.GatewayStreamRequest.{Subscribe, SubscribeObserveEvents, SubscribeWithPattern}
import esw.ocs.api.protocol.OkOrUnhandledResponse

// ESW-278 Contract samples for gateway service. These samples are also used in `RoundTripTest`
object GatewayContract extends GatewayCodecs with GatewayData {

  private val models: ModelSet = ModelSet.models(
    ModelType[Event](observeEvent, systemEvent, wfsObserveEvent, opticalDetObserveEvent),
    ModelType(alarmKey),
    ModelType(AlarmSeverity),
    ModelType(prefix),
    ModelType(eventKey),
    ModelType(componentId),
    ModelType(logMetadata),
    ModelType(Subsystem),
    ModelType[GatewayException](
      invalidComponent,
      emptyEventKeys,
      eventServerUnavailable,
      invalidMaxFrequency,
      setAlarmSeverityFailure
    ),
    ModelType(Level),
    ModelType(ContainerLifecycleState),
    ModelType(SupervisorLifecycleState)
  )

  private val httpRequests = new RequestSet[GatewayRequest] {
    requestType(postComponentCommand)
    requestType(postSequencerCommand)
    requestType(publishEvent)
    requestType(getEvent)
    requestType(setAlarmSeverity)
    requestType(log)
    requestType(setLogLevel)
    requestType(getLogMetadata)
    requestType(gateWayReqGoOnline)
    requestType(gateWayReqGoOffline)
    requestType(shutdown)
    requestType(restart)
    requestType(getComponentLifecycleState)
    requestType(getContainerLifecycleState)
  }

  private val websocketRequests = new RequestSet[GatewayStreamRequest] {
    requestType(websocketComponentCommand)
    requestType(websocketSequencerCommand)
    requestType(subscribe)
    requestType(subscribeWithPattern)
    requestType(subscribeObserveEvents)
  }

  private val httpEndpoints: List[Endpoint] = List(
    Endpoint(
      name[ComponentCommand],
      name[ValidateResponse],
      List(name[InvalidComponent]),
      Some(
        "Response type will depend on the command passed to componentCommand request. For all possible request and response type mappings refer to HTTP endpoint documentation of command service in CSW."
      )
    ),
    Endpoint(
      name[SequencerCommand],
      name[OkOrUnhandledResponse],
      List(name[InvalidComponent]),
      Some(
        "Response type will depend on the command passed to sequencerCommand request. For all possible request and response type mappings refer to HTTP endpoint documentation of sequencer service in ESW."
      )
    ),
    Endpoint(name[PublishEvent], name[Done], List(name[EventServerUnavailable])),
    Endpoint(name[GetEvent], arrayName[Event], List(name[EmptyEventKeys], name[EventServerUnavailable])),
    Endpoint(name[SetAlarmSeverity], name[Done], List(name[SetAlarmSeverityFailure])),
    Endpoint(name[Log], name[Done]),
    Endpoint(name[SetLogLevel], name[Done]),
    Endpoint(name[GetLogMetadata], name[LogMetadata]),
    Endpoint(name[Shutdown], name[Done]),
    Endpoint(name[Restart], name[Done]),
    Endpoint(name[GoOnline], name[Done]),
    Endpoint(name[GoOffline], name[Done]),
    Endpoint(name[GetContainerLifecycleState], name[ContainerLifecycleState]),
    Endpoint(name[GetComponentLifecycleState], name[SupervisorLifecycleState])
  )

  private val webSocketEndpoints: List[Endpoint] = List(
    Endpoint(
      name[GatewayStreamRequest.ComponentCommand],
      name[SubmitResponse],
      description = Some(
        "Response type will depend on the command passed to componentCommand request. For all possible request and response type mappings refer to websocket endpoint documentation of command service in CSW."
      )
    ),
    Endpoint(
      name[GatewayStreamRequest.SequencerCommand],
      name[SubmitResponse],
      description = Some(
        "Response type will depend on the command passed to sequencerCommand request. For all possible request and response type mappings refer to websocket endpoint documentation of sequencer service in ESW."
      )
    ),
    Endpoint(name[Subscribe], name[Event], List(name[EmptyEventKeys], name[InvalidMaxFrequency])),
    Endpoint(name[SubscribeWithPattern], name[Event], List(name[InvalidMaxFrequency])),
    Endpoint(name[SubscribeObserveEvents], name[Event], List(name[InvalidMaxFrequency]))
  )

  private val readme: Readme = Readme(ResourceFetcher.getResourceAsString("gateway-service/README.md"))

  val service: Service = Service(
    `http-contract` = Contract(httpEndpoints, httpRequests),
    `websocket-contract` = Contract(webSocketEndpoints, websocketRequests),
    models = models,
    readme = readme
  )
}
