package esw.ocs.dsl.params

import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.params.core.models.{ExposureIdType, ObsId}
import csw.params.events.{EventName, ObserveEvent}
import csw.prefix.models.Prefix

private[dsl] class SequencerObserveEvent(prefix: Prefix) {
  private def createObserveEvent(eventName: EventName, obsId: ObsId) =
    ObserveEvent(prefix, eventName, Set(StringKey.make("obsId").set(obsId.toString())))

  private def createObserveEventWithExposureId(eventName: EventName, obsId: ObsId, exposureId: ExposureIdType) = {
    val paramset: Set[Parameter[_]] = Set(
      StringKey.make("obsId").set(obsId.toString()),
      StringKey.make("exposureId").set(exposureId.toString)
    )
    ObserveEvent(prefix, eventName, paramset)
  }

  def presetStart(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName(SequencerObserveEventName.PresetStart), obsId)
  def presetEnd(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName(SequencerObserveEventName.PresetEnd), obsId)
  def guidstarAcqStart(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName(SequencerObserveEventName.GuidstarAcqStart), obsId)
  def guidstarAcqEnd(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName(SequencerObserveEventName.GuidstarAcqEnd), obsId)
  def scitargetAcqStart(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName(SequencerObserveEventName.ScitargetAcqStart), obsId)
  def scitargetAcqEnd(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName(SequencerObserveEventName.ScitargetAcqEnd), obsId)
  def observationStart(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName(SequencerObserveEventName.ObservationStart), obsId)
  def observationEnd(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName(SequencerObserveEventName.ObservationEnd), obsId)
  def observeStart(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName(SequencerObserveEventName.ObserveStart), obsId)
  def observeEnd(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName(SequencerObserveEventName.ObserveEnd), obsId)

  def exposureStart(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(EventName(SequencerObserveEventName.ExposureStart), obsId, exposureId)
  def exposureEnd(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(EventName(SequencerObserveEventName.ExposureEnd), obsId, exposureId)
  def readoutEnd(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(EventName(SequencerObserveEventName.ReadoutEnd), obsId, exposureId)
  def readoutFailed(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(EventName(SequencerObserveEventName.ReadoutFailed), obsId, exposureId)
  def dataWriteStart(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(EventName(SequencerObserveEventName.DataWriteStart), obsId, exposureId)
  def dataWriteEnd(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(EventName(SequencerObserveEventName.DataWriteEnd), obsId, exposureId)
  def prepareStart(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(EventName(SequencerObserveEventName.PrepareStart), obsId, exposureId)

  def observePaused(): ObserveEvent  = ObserveEvent(prefix, EventName(SequencerObserveEventName.ObservePaused))
  def observeResumed(): ObserveEvent = ObserveEvent(prefix, EventName(SequencerObserveEventName.ObserveResumed))

  def downtimeStart(obsId: ObsId, reasonForDowntime: String): ObserveEvent = {
    val obsIdParam          = StringKey.make("obsId").set(obsId.toString())
    val downtimeReasonParam = StringKey.make("reason").set(reasonForDowntime)
    ObserveEvent(prefix, EventName(SequencerObserveEventName.DowntimeStart), Set(obsIdParam, downtimeReasonParam))
  }
}
