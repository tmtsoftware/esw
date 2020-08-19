package esw.contract.data.gateway

import akka.Done
import csw.alarm.models.AlarmSeverity
import csw.contract.ResourceFetcher
import csw.contract.generator.ClassNameHelpers._
import csw.contract.generator._
import csw.logging.models.{Level, LogMetadata}
import csw.params.commands.CommandResponse.{SubmitResponse, ValidateResponse}
import csw.params.events.Event
import csw.prefix.models.Subsystem
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.PostRequest.{
  ComponentCommand,
  GetEvent,
  GetLogMetadata,
  Log,
  PublishEvent,
  SequencerCommand,
  SetAlarmSeverity,
  SetLogLevel
}
import esw.gateway.api.protocol.WebsocketRequest.{Subscribe, SubscribeWithPattern}
import esw.gateway.api.protocol._
import esw.ocs.api.protocol.OkOrUnhandledResponse

object GatewayContract extends GatewayCodecs with GatewayData {

  private val models: ModelSet = ModelSet.models(
    ModelType[Event](observeEvent, systemEvent),
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
    ModelType(Level)
  )

  private val httpRequests = new RequestSet[PostRequest] {
    requestType(postComponentCommand)
    requestType(postSequencerCommand)
    requestType(publishEvent)
    requestType(getEvent)
    requestType(setAlarmSeverity)
    requestType(log)
    requestType(setLogLevel)
    requestType(getLogMetadata)
  }

  private val websocketRequests = new RequestSet[WebsocketRequest] {
    requestType(websocketComponentCommand)
    requestType(websocketSequencerCommand)
    requestType(subscribe)
    requestType(subscribeWithPattern)
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
    Endpoint(name[SetLogLevel], name[Unit]),
    Endpoint(name[GetLogMetadata], name[LogMetadata])
  )

  private val webSocketEndpoints: List[Endpoint] = List(
    Endpoint(
      name[WebsocketRequest.ComponentCommand],
      name[SubmitResponse],
      description = Some(
        "Response type will depend on the command passed to componentCommand request. For all possible request and response type mappings refer to websocket endpoint documentation of command service in CSW."
      )
    ),
    Endpoint(
      name[WebsocketRequest.SequencerCommand],
      name[SubmitResponse],
      description = Some(
        "Response type will depend on the command passed to sequencerCommand request. For all possible request and response type mappings refer to websocket endpoint documentation of sequencer service in ESW."
      )
    ),
    Endpoint(name[Subscribe], name[Event], List(name[EmptyEventKeys], name[InvalidMaxFrequency])),
    Endpoint(name[SubscribeWithPattern], name[Event], List(name[InvalidMaxFrequency]))
  )

  private val readme: Readme = Readme(ResourceFetcher.getResourceAsString("gateway-service/README.md"))

  val service: Service = Service(
    `http-contract` = Contract(httpEndpoints, httpRequests),
    `websocket-contract` = Contract(webSocketEndpoints, websocketRequests),
    models = models,
    readme = readme
  )
}
