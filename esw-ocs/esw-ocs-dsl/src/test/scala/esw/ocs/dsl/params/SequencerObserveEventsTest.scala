package esw.ocs.dsl.params

import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId
import csw.params.events.EventName
import csw.prefix.models.Prefix
import esw.ocs.dsl.params.SequencerObserveEvent._
import esw.testcommons.BaseTestSuite
import org.scalatest.prop.TableDrivenPropertyChecks._

class SequencerObserveEventsTest extends BaseTestSuite {
  "SequencerObserveEvents" must {
    val obsId                         = randomString(10)
    val exposureId                    = randomString(10)
    val prefixStr                     = Prefix(randomSubsystem, randomString(5)).toString()
    val obsIdParam: Parameter[_]      = StringKey.make("obsId").set(obsId)
    val exposureIdParam: Parameter[_] = StringKey.make("exposureId").set(exposureId)

    "create Observe Event with obsId parameters | ESW-81" in {
      Table(
        ("Observe Event", "Event Name", "Prefix"),
        (PresetStart.make(prefixStr, ObsId(obsId)), PresetStart.entryName, prefixStr),
        (PresetStart.make(prefixStr, ObsId(obsId)), PresetStart.entryName, prefixStr),
        (PresetEnd.make(prefixStr, ObsId(obsId)), PresetEnd.entryName, prefixStr),
        (GuidstarAcqStart.make(prefixStr, ObsId(obsId)), GuidstarAcqStart.entryName, prefixStr),
        (GuidstarAcqEnd.make(prefixStr, ObsId(obsId)), GuidstarAcqEnd.entryName, prefixStr),
        (ScitargetAcqStart.make(prefixStr, ObsId(obsId)), ScitargetAcqStart.entryName, prefixStr),
        (ScitargetAcqEnd.make(prefixStr, ObsId(obsId)), ScitargetAcqEnd.entryName, prefixStr),
        (ObservationStart.make(prefixStr, ObsId(obsId)), ObservationStart.entryName, prefixStr),
        (ObservationEnd.make(prefixStr, ObsId(obsId)), ObservationEnd.entryName, prefixStr),
        (ObserveStart.make(prefixStr, ObsId(obsId)), ObserveStart.entryName, prefixStr),
        (ObserveEnd.make(prefixStr, ObsId(obsId)), ObserveEnd.entryName, prefixStr)
      ).forEvery((observeEvent, expectedEventName, expectedPrefixStr) => {
        observeEvent.eventName should ===(EventName(expectedEventName))
        observeEvent.source should ===(Prefix(expectedPrefixStr))
        observeEvent.paramSet shouldBe Set(obsIdParam)
      })
    }

    "create Observe Event with obsId and exposure Id parameters | ESW-81" in {
      Table(
        ("Observe Event", "Event Name", "Prefix"),
        (ExposureStart.make(prefixStr, ObsId(obsId), exposureId), ExposureStart.entryName, prefixStr),
        (ExposureEnd.make(prefixStr, ObsId(obsId), exposureId), ExposureEnd.entryName, prefixStr),
        (readoutEnd.make(prefixStr, ObsId(obsId), exposureId), readoutEnd.entryName, prefixStr),
        (readoutFailed.make(prefixStr, ObsId(obsId), exposureId), readoutFailed.entryName, prefixStr),
        (dataWriteStart.make(prefixStr, ObsId(obsId), exposureId), dataWriteStart.entryName, prefixStr),
        (dataWriteEnd.make(prefixStr, ObsId(obsId), exposureId), dataWriteEnd.entryName, prefixStr),
        (PrepareStart.make(prefixStr, ObsId(obsId), exposureId), PrepareStart.entryName, prefixStr)
      ).forEvery((observeEvent, expectedEventName, expectedPrefixStr) => {
        observeEvent.eventName should ===(EventName(expectedEventName))
        observeEvent.source should ===(Prefix(expectedPrefixStr))
        observeEvent.paramSet shouldBe Set(obsIdParam, exposureIdParam)
      })
    }

    "create Observe Event with fixed Parameter set | ESW-81" in {
      val event = DowntimeStart.make(prefixStr, ObsId(obsId), "bad weather")
      event.eventName should ===(EventName(DowntimeStart.entryName))
      event.source should ===(Prefix(prefixStr))
      event.paramSet shouldBe Set(obsIdParam, StringKey.make("reason").set("bad weather"))
    }
  }
}
