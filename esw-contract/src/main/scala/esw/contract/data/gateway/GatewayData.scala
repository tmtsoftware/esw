package esw.contract.data.gateway

import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.location.api.models.{ComponentId, ComponentType}
import csw.logging.models.{Level, LogMetadata}
import csw.params.core.models.ObsId
import csw.params.events.{EventKey, EventName, IRDetectorEvent, ObserveEvent, OpticalDetectorEvent, SystemEvent, WFSDetectorEvent}
import csw.prefix.models.Subsystem
import esw.contract.data.sequencer.SequencerData
import esw.gateway.api.protocol.GatewayRequest._
import esw.gateway.api.protocol.GatewayStreamRequest.{Subscribe, SubscribeWithPattern}
import esw.gateway.api.protocol._

trait GatewayData extends SequencerData {
  val componentId: ComponentId = ComponentId(prefix, ComponentType.HCD)
  val eventName: EventName     = EventName("offline")
  private val obsId1: ObsId    = ObsId("1234A-432-123")

  val observeEvent: ObserveEvent           = IRDetectorEvent.observeStart(prefix, obsId1)
  val wfsObserveEvent: ObserveEvent        = WFSDetectorEvent.publishSuccess(prefix)
  val opticalDetObserveEvent: ObserveEvent = OpticalDetectorEvent.observeStart(prefix, obsId1)

  val systemEvent: SystemEvent = SystemEvent(prefix, eventName)
  val eventKey: EventKey       = EventKey(prefix, eventName)

  val logMetadata: LogMetadata = LogMetadata(Level.INFO, Level.DEBUG, Level.INFO, Level.ERROR)

  val postComponentCommand: ComponentCommand                 = ComponentCommand(componentId, observeValidate)
  val postSequencerCommand: SequencerCommand                 = SequencerCommand(componentId, prepend)
  val publishEvent: PublishEvent                             = PublishEvent(observeEvent)
  val getEvent: GetEvent                                     = GetEvent(Set(eventKey))
  val alarmKey: AlarmKey                                     = AlarmKey(prefix, "someAlarm")
  val setAlarmSeverity: SetAlarmSeverity                     = SetAlarmSeverity(alarmKey, AlarmSeverity.Okay)
  val log: Log                                               = Log(prefix, Level.DEBUG, "message", Map("additional-info" -> 45))
  val setLogLevel: SetLogLevel                               = SetLogLevel(componentId, Level.ERROR)
  val getLogMetadata: GetLogMetadata                         = GetLogMetadata(componentId)
  val shutdown: Shutdown                                     = Shutdown(componentId)
  val restart: Restart                                       = Restart(componentId)
  val gateWayReqGoOffline: GoOffline                         = GoOffline(componentId)
  val gateWayReqGoOnline: GoOnline                           = GoOnline(componentId)
  val getComponentLifecycleState: GetComponentLifecycleState = GetComponentLifecycleState(componentId)
  val getContainerLifecycleState: GetContainerLifecycleState = GetContainerLifecycleState(prefix)

  val websocketComponentCommand: GatewayStreamRequest.ComponentCommand =
    GatewayStreamRequest.ComponentCommand(componentId, queryFinal)
  val websocketSequencerCommand: GatewayStreamRequest.SequencerCommand =
    GatewayStreamRequest.SequencerCommand(componentId, sequencerQueryFinal)
  val subscribe: Subscribe                       = Subscribe(Set(eventKey), Some(10))
  val subscribeWithPattern: SubscribeWithPattern = SubscribeWithPattern(Subsystem.CSW, Some(10), "[a-b]*")

  val invalidComponent: InvalidComponent               = InvalidComponent("invalid component")
  val emptyEventKeys: EmptyEventKeys                   = EmptyEventKeys()
  val eventServerUnavailable: EventServerUnavailable   = EventServerUnavailable()
  val invalidMaxFrequency: InvalidMaxFrequency         = InvalidMaxFrequency()
  val setAlarmSeverityFailure: SetAlarmSeverityFailure = SetAlarmSeverityFailure("alarm fail")
}
