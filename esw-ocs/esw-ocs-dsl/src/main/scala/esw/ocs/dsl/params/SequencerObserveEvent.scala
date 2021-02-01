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
    createObserveEvent(EventName("PresetStart"), obsId)
  def presetEnd(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName("PresetEnd"), obsId)
  def guidstarAcqStart(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName("GuidstarAcqStart"), obsId)
  def guidstarAcqEnd(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName("GuidstarAcqEnd"), obsId)
  def scitargetAcqStart(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName("ScitargetAcqStart"), obsId)
  def scitargetAcqEnd(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName("ScitargetAcqEnd"), obsId)
  def observationStart(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName("ObservationStart"), obsId)
  def observationEnd(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName("ObservationEnd"), obsId)
  def observeStart(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName("ObserveStart"), obsId)
  def observeEnd(obsId: ObsId): ObserveEvent =
    createObserveEvent(EventName("ObserveEnd"), obsId)

  def exposureStart(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(EventName("ExposureStart"), obsId, exposureId)
  def exposureEnd(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(EventName("ExposureEnd"), obsId, exposureId)
  def readoutEnd(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(EventName("ReadoutEnd"), obsId, exposureId)
  def readoutFailed(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(EventName("ReadoutFailed"), obsId, exposureId)
  def dataWriteStart(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(EventName("DataWriteStart"), obsId, exposureId)
  def dataWriteEnd(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(EventName("DataWriteEnd"), obsId, exposureId)
  def prepareStart(obsId: ObsId, exposureId: ExposureIdType): ObserveEvent =
    createObserveEventWithExposureId(EventName("PrepareStart"), obsId, exposureId)

  def observePaused(): ObserveEvent  = ObserveEvent(prefix, EventName("ObservePaused"))
  def observeResumed(): ObserveEvent = ObserveEvent(prefix, EventName("ObserveResumed"))

  def downtimeStart(obsId: ObsId, reasonForDowntime: String): ObserveEvent = {
    val obsIdParam          = StringKey.make("obsId").set(obsId.toString())
    val downtimeReasonParam = StringKey.make("reason").set(reasonForDowntime)
    ObserveEvent(prefix, EventName("DowntimeStart"), Set(obsIdParam, downtimeReasonParam))
  }
}
