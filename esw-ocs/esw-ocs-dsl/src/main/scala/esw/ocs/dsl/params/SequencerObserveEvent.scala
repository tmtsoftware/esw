package esw.ocs.dsl.params

import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.params.events.{EventName, ObserveEvent}
import csw.prefix.models.Prefix

sealed trait SequencerObserveEvent {
  protected def name: String = this.getClass.getSimpleName.dropRight(1)
}

sealed trait ObserveEvents extends SequencerObserveEvent {
  def make(sourcePrefix: String, obsId: ObsId): ObserveEvent =
    ObserveEvent(Prefix(sourcePrefix), EventName(this.name), Set(StringKey.make("obsId").set(obsId.obsId)))
}

sealed trait ObserveEventsWithExposureId extends SequencerObserveEvent {
  def make(sourcePrefix: String, obsId: ObsId, exposureId: String): ObserveEvent = {
    val paramset: Set[Parameter[_]] = Set(
      StringKey.make("obsId").set(obsId.obsId),
      StringKey.make("exposureId").set(exposureId)
    )
    ObserveEvent(Prefix(sourcePrefix), EventName(this.name), paramset)
  }
}

object SequencerObserveEvent {

  case object PresetStart       extends ObserveEvents
  case object PresetEnd         extends ObserveEvents
  case object GuidstarAcqStart  extends ObserveEvents
  case object GuidstarAcqEnd    extends ObserveEvents
  case object ScitargetAcqStart extends ObserveEvents
  case object ScitargetAcqEnd   extends ObserveEvents
  case object ObservationStart  extends ObserveEvents
  case object ObservationEnd    extends ObserveEvents
  case object ObserveStart      extends ObserveEvents
  case object ObserveEnd        extends ObserveEvents

  case object ExposureStart  extends ObserveEventsWithExposureId
  case object ExposureEnd    extends ObserveEventsWithExposureId
  case object readoutEnd     extends ObserveEventsWithExposureId
  case object readoutFailed  extends ObserveEventsWithExposureId
  case object dataWriteStart extends ObserveEventsWithExposureId
  case object dataWriteEnd   extends ObserveEventsWithExposureId
  case object PrepareStart   extends ObserveEventsWithExposureId

  case object ObservePaused  extends SequencerObserveEvent
  case object ObserveResumed extends SequencerObserveEvent
  case object DowntimeStart extends SequencerObserveEvent {
    def make(sourcePrefix: String, obsId: ObsId, reasonForDowntime: String): ObserveEvent = {
      val obsIdParam          = StringKey.make("obsId").set(obsId.obsId)
      val downtimeReasonParam = StringKey.make("reason").set(reasonForDowntime)
      ObserveEvent(Prefix(sourcePrefix), EventName(this.name), Set(obsIdParam, downtimeReasonParam))
    }
  }
}

// created for consumption from Kotlin
object ObserveEventFactory {
  val PresetStart       = SequencerObserveEvent.PresetStart
  val PresetEnd         = SequencerObserveEvent.PresetEnd
  val GuidstarAcqStart  = SequencerObserveEvent.GuidstarAcqStart
  val GuidstarAcqEnd    = SequencerObserveEvent.GuidstarAcqEnd
  val ScitargetAcqStart = SequencerObserveEvent.ScitargetAcqStart
  val ScitargetAcqEnd   = SequencerObserveEvent.ScitargetAcqEnd
  val ObservationStart  = SequencerObserveEvent.ObservationStart
  val ObservationEnd    = SequencerObserveEvent.ObservationEnd
  val ObserveStart      = SequencerObserveEvent.ObserveStart
  val ObserveEnd        = SequencerObserveEvent.ObserveEnd
  val ExposureStart     = SequencerObserveEvent.ExposureStart
  val ExposureEnd       = SequencerObserveEvent.ExposureEnd
  val readoutEnd        = SequencerObserveEvent.readoutEnd
  val readoutFailed     = SequencerObserveEvent.readoutFailed
  val dataWriteStart    = SequencerObserveEvent.dataWriteStart
  val dataWriteEnd      = SequencerObserveEvent.dataWriteEnd
  val PrepareStart      = SequencerObserveEvent.PrepareStart
  val ObservePaused     = SequencerObserveEvent.ObservePaused
  val ObserveResumed    = SequencerObserveEvent.ObserveResumed
  val DowntimeStart     = SequencerObserveEvent.DowntimeStart
}
