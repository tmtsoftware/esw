package esw.ocs.dsl.params

object SequencerObserveEventName {
  private val ObserveEventNamePrefix = "ObserveEvent"
  private val Separator              = "."

  val PresetStart       = s"${ObserveEventNamePrefix}${Separator}PresetStart"
  val PresetEnd         = s"${ObserveEventNamePrefix}${Separator}PresetEnd"
  val GuidstarAcqStart  = s"${ObserveEventNamePrefix}${Separator}GuidstarAcqStart"
  val GuidstarAcqEnd    = s"${ObserveEventNamePrefix}${Separator}GuidstarAcqEnd"
  val ScitargetAcqStart = s"${ObserveEventNamePrefix}${Separator}ScitargetAcqStart"
  val ScitargetAcqEnd   = s"${ObserveEventNamePrefix}${Separator}ScitargetAcqEnd"
  val ObservationStart  = s"${ObserveEventNamePrefix}${Separator}ObservationStart"
  val ObservationEnd    = s"${ObserveEventNamePrefix}${Separator}ObservationEnd"
  val ObserveStart      = s"${ObserveEventNamePrefix}${Separator}ObserveStart"
  val ObserveEnd        = s"${ObserveEventNamePrefix}${Separator}ObserveEnd"
  val ExposureStart     = s"${ObserveEventNamePrefix}${Separator}ExposureStart"
  val ExposureEnd       = s"${ObserveEventNamePrefix}${Separator}ExposureEnd"
  val ReadoutEnd        = s"${ObserveEventNamePrefix}${Separator}ReadoutEnd"
  val ReadoutFailed     = s"${ObserveEventNamePrefix}${Separator}ReadoutFailed"
  val DataWriteStart    = s"${ObserveEventNamePrefix}${Separator}DataWriteStart"
  val DataWriteEnd      = s"${ObserveEventNamePrefix}${Separator}DataWriteEnd"
  val PrepareStart      = s"${ObserveEventNamePrefix}${Separator}PrepareStart"
  val ObservePaused     = s"${ObserveEventNamePrefix}${Separator}ObservePaused"
  val ObserveResumed    = s"${ObserveEventNamePrefix}${Separator}ObserveResumed"
  val DowntimeStart     = s"${ObserveEventNamePrefix}${Separator}DowntimeStart"
}
