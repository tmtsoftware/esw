package esw.ocs.dsl.params

import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.params.events.{EventName, ObserveEvent}
import csw.prefix.models.Prefix

object SequencerObserveEvent {
  private def createObserveEvent(sourcePrefix: String, eventName: EventName, obsId: ObsId) =
    ObserveEvent(Prefix(sourcePrefix), eventName, Set(StringKey.make("obsId").set(obsId.obsId)))

  private def createObserveEventWithExposureId(
      sourcePrefix: String,
      eventName: EventName,
      obsId: ObsId,
      exposureId: String
  ): ObserveEvent = {
    val paramset: Set[Parameter[_]] = Set(
      StringKey.make("obsId").set(obsId.obsId),
      StringKey.make("exposureId").set(exposureId)
    )
    ObserveEvent(Prefix(sourcePrefix), eventName, paramset)
  }

  def presetStart(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    createObserveEvent(sourcePrefix, EventName("PresetStart"), obsId)
  def presetEnd(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    createObserveEvent(sourcePrefix, EventName("PresetEnd"), obsId)
  def guidstarAcqStart(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    createObserveEvent(sourcePrefix, EventName("GuidstarAcqStart"), obsId)
  def guidstarAcqEnd(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    createObserveEvent(sourcePrefix, EventName("GuidstarAcqEnd"), obsId)
  def scitargetAcqStart(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    createObserveEvent(sourcePrefix, EventName("ScitargetAcqStart"), obsId)
  def scitargetAcqEnd(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    createObserveEvent(sourcePrefix, EventName("ScitargetAcqEnd"), obsId)
  def observationStart(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    createObserveEvent(sourcePrefix, EventName("ObservationStart"), obsId)
  def observationEnd(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    createObserveEvent(sourcePrefix, EventName("ObservationEnd"), obsId)
  def observeStart(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    createObserveEvent(sourcePrefix, EventName("ObserveStart"), obsId)
  def observeEnd(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    createObserveEvent(sourcePrefix, EventName("ObserveEnd"), obsId)

  def exposureStart(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    createObserveEventWithExposureId(sourcePrefix, EventName("ExposureStart"), obsId, exposureId)
  def exposureEnd(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    createObserveEventWithExposureId(sourcePrefix, EventName("ExposureEnd"), obsId, exposureId)
  def readoutEnd(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    createObserveEventWithExposureId(sourcePrefix, EventName("ReadoutEnd"), obsId, exposureId)
  def readoutFailed(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    createObserveEventWithExposureId(sourcePrefix, EventName("ReadoutFailed"), obsId, exposureId)
  def dataWriteStart(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    createObserveEventWithExposureId(sourcePrefix, EventName("DataWriteStart"), obsId, exposureId)
  def dataWriteEnd(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    createObserveEventWithExposureId(sourcePrefix, EventName("DataWriteEnd"), obsId, exposureId)
  def prepareStart(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent =
    createObserveEventWithExposureId(sourcePrefix, EventName("PrepareStart"), obsId, exposureId)

  def observePaused(sourcePrefix: String): ObserveEvent  = ObserveEvent(Prefix(sourcePrefix), EventName("ObservePaused"))
  def observeResumed(sourcePrefix: String): ObserveEvent = ObserveEvent(Prefix(sourcePrefix), EventName("ObserveResumed"))

  def downtimeStart(sourcePrefix: String, obsId: ObsId, reasonForDowntime: String): ObserveEvent = {
    val obsIdParam          = StringKey.make("obsId").set(obsId.obsId)
    val downtimeReasonParam = StringKey.make("reason").set(reasonForDowntime)
    ObserveEvent(Prefix(sourcePrefix), EventName("DowntimeStart"), Set(obsIdParam, downtimeReasonParam))
  }
}
