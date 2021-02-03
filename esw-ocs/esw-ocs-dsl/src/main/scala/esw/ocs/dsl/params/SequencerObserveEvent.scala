package esw.ocs.dsl.params

import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.params.core.models.{ExposureIdType, ObsId}
import csw.params.events.{EventName, ObserveEvent}
import csw.prefix.models.Prefix

private[dsl] class SequencerObserveEvent(prefix: Prefix) {

  def presetStart(obsId: ObsId): ObserveEvent       = createObserveEvent(SequencerObserveEventNames.PresetStart, obsId)
  def presetEnd(obsId: ObsId): ObserveEvent         = createObserveEvent(SequencerObserveEventNames.PresetEnd, obsId)
  def guidstarAcqStart(obsId: ObsId): ObserveEvent  = createObserveEvent(SequencerObserveEventNames.GuidstarAcqStart, obsId)
  def guidstarAcqEnd(obsId: ObsId): ObserveEvent    = createObserveEvent(SequencerObserveEventNames.GuidstarAcqEnd, obsId)
  def scitargetAcqStart(obsId: ObsId): ObserveEvent = createObserveEvent(SequencerObserveEventNames.ScitargetAcqStart, obsId)
  def scitargetAcqEnd(obsId: ObsId): ObserveEvent   = createObserveEvent(SequencerObserveEventNames.ScitargetAcqEnd, obsId)
  def observationStart(obsId: ObsId): ObserveEvent  = createObserveEvent(SequencerObserveEventNames.ObservationStart, obsId)
  def observationEnd(obsId: ObsId): ObserveEvent    = createObserveEvent(SequencerObserveEventNames.ObservationEnd, obsId)
  def observeStart(obsId: ObsId): ObserveEvent      = createObserveEvent(SequencerObserveEventNames.ObserveStart, obsId)
  def observeEnd(obsId: ObsId): ObserveEvent        = createObserveEvent(SequencerObserveEventNames.ObserveEnd, obsId)

  def exposureStart(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(SequencerObserveEventNames.ExposureStart, obsId, exposureId)
  def exposureEnd(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(SequencerObserveEventNames.ExposureEnd, obsId, exposureId)
  def readoutEnd(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(SequencerObserveEventNames.ReadoutEnd, obsId, exposureId)
  def readoutFailed(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(SequencerObserveEventNames.ReadoutFailed, obsId, exposureId)
  def dataWriteStart(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(SequencerObserveEventNames.DataWriteStart, obsId, exposureId)
  def dataWriteEnd(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(SequencerObserveEventNames.DataWriteEnd, obsId, exposureId)
  def prepareStart(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(SequencerObserveEventNames.PrepareStart, obsId, exposureId)

  def observePaused(): ObserveEvent  = ObserveEvent(prefix, SequencerObserveEventNames.ObservePaused)
  def observeResumed(): ObserveEvent = ObserveEvent(prefix, SequencerObserveEventNames.ObserveResumed)

  def downtimeStart(obsId: ObsId, reasonForDowntime: String): ObserveEvent = {
    val obsIdParam          = StringKey.make("obsId").set(obsId.toString())
    val downtimeReasonParam = StringKey.make("reason").set(reasonForDowntime)
    ObserveEvent(prefix, SequencerObserveEventNames.DowntimeStart, Set(obsIdParam, downtimeReasonParam))
  }

  private def createObserveEvent(eventName: EventName, obsId: ObsId) =
    ObserveEvent(prefix, eventName, Set(StringKey.make("obsId").set(obsId.toString())))

  private def createObserveEventWithExposureId(eventName: EventName, obsId: ObsId, exposureId: ExposureIdType) = {
    val paramset: Set[Parameter[_]] = Set(
      StringKey.make("obsId").set(obsId.toString()),
      StringKey.make("exposureId").set(exposureId.toString)
    )
    ObserveEvent(prefix, eventName, paramset)
  }
}
