package esw.ocs.dsl.params

import csw.params.events.EventName

object SequencerObserveEventNames {

  val PresetStart: EventName       = eventName("PresetStart")
  val PresetEnd: EventName         = eventName("PresetEnd")
  val GuidstarAcqStart: EventName  = eventName("GuidstarAcqStart")
  val GuidstarAcqEnd: EventName    = eventName("GuidstarAcqEnd")
  val ScitargetAcqStart: EventName = eventName("ScitargetAcqStart")
  val ScitargetAcqEnd: EventName   = eventName("ScitargetAcqEnd")
  val ObservationStart: EventName  = eventName("ObservationStart")
  val ObservationEnd: EventName    = eventName("ObservationEnd")
  val ObserveStart: EventName      = eventName("ObserveStart")
  val ObserveEnd: EventName        = eventName("ObserveEnd")
  val ExposureStart: EventName     = eventName("ExposureStart")
  val ExposureEnd: EventName       = eventName("ExposureEnd")
  val ReadoutEnd: EventName        = eventName("ReadoutEnd")
  val ReadoutFailed: EventName     = eventName("ReadoutFailed")
  val DataWriteStart: EventName    = eventName("DataWriteStart")
  val DataWriteEnd: EventName      = eventName("DataWriteEnd")
  val PrepareStart: EventName      = eventName("PrepareStart")
  val ObservePaused: EventName     = eventName("ObservePaused")
  val ObserveResumed: EventName    = eventName("ObserveResumed")
  val DowntimeStart: EventName     = eventName("DowntimeStart")

  private def eventName(name: String) = EventName(s"ObserveEvent.$name")
}
