package esw.ocs.dsl.params

import csw.params.core.generics.KeyType.StringKey
import csw.params.core.models.ObsId
import csw.params.events.EventName
import csw.prefix.models.Prefix
import esw.ocs.dsl.params.SequencerObserveEvent._
import esw.testcommons.BaseTestSuite
import org.scalatest.prop.TableDrivenPropertyChecks._

class SequencerObserveEventsTest extends BaseTestSuite {
  "SequencerObserveEvents" must {

    "create Observe Event with obsId parameters | ESW-81" in {
      Table(
        ("Observe Event", "Event Name", "Prefix"),
        (PresetStart.create("ESW.test", ObsId("some-id")), PresetStart.entryName, "ESW.test"),
        (PresetEnd.create("ESW.test", ObsId("some-id")), PresetEnd.entryName, "ESW.test"),
        (GuidstarAcqStart.create("ESW.test", ObsId("some-id")), GuidstarAcqStart.entryName, "ESW.test"),
        (GuidstarAcqEnd.create("ESW.test", ObsId("some-id")), GuidstarAcqEnd.entryName, "ESW.test"),
        (ScitargetAcqStart.create("ESW.test", ObsId("some-id")), ScitargetAcqStart.entryName, "ESW.test"),
        (ScitargetAcqEnd.create("ESW.test", ObsId("some-id")), ScitargetAcqEnd.entryName, "ESW.test"),
        (ObservationStart.create("ESW.test", ObsId("some-id")), ObservationStart.entryName, "ESW.test"),
        (ObservationEnd.create("ESW.test", ObsId("some-id")), ObservationEnd.entryName, "ESW.test"),
        (ObserveStart.create("ESW.test", ObsId("some-id")), ObserveStart.entryName, "ESW.test"),
        (ObserveEnd.create("ESW.test", ObsId("some-id")), ObserveEnd.entryName, "ESW.test")
      ).forEvery((observeEvent, expectedEventName, expectedPrefixStr) => {
        observeEvent.eventName shouldBe EventName(expectedEventName)
        observeEvent.source shouldBe Prefix(expectedPrefixStr)
      })
    }

    "create Observe Event with obsId and exposure Id parameters | ESW-81" in {
      Table(
        ("Observe Event", "Event Name", "Prefix"),
        (ExposureStart.create("ESW.test", ObsId("some-id"), "exposureId1"), ExposureStart.entryName, "ESW.test"),
        (ExposureEnd.create("ESW.test", ObsId("some-id"), "exposureId1"), ExposureEnd.entryName, "ESW.test"),
        (readoutEnd.create("ESW.test", ObsId("some-id"), "exposureId1"), readoutEnd.entryName, "ESW.test"),
        (readoutFailed.create("ESW.test", ObsId("some-id"), "exposureId1"), readoutFailed.entryName, "ESW.test"),
        (dataWriteStart.create("ESW.test", ObsId("some-id"), "exposureId1"), dataWriteStart.entryName, "ESW.test"),
        (dataWriteEnd.create("ESW.test", ObsId("some-id"), "exposureId1"), dataWriteEnd.entryName, "ESW.test"),
        (PrepareStart.create("ESW.test", ObsId("some-id"), "exposureId1"), PrepareStart.entryName, "ESW.test")
      ).forEvery((observeEvent, expectedEventName, expectedPrefixStr) => {
        observeEvent.eventName shouldBe EventName(expectedEventName)
        observeEvent.source shouldBe Prefix(expectedPrefixStr)
      })
    }

    "create Observe Event with fixed Parameter set | ESW-81" in {
      val event = DowntimeStart.create("ESW.test", ObsId("some-id"), "bad weather")
      event.eventName shouldBe EventName(DowntimeStart.entryName)
      event.source shouldBe Prefix("ESW.test")
      event.paramSet shouldBe Set(StringKey.make("reason").set("bad weather"))
    }
  }
}
