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
        (PresetStart.make(prefixStr, ObsId(obsId)), "PresetStart", prefixStr),
        (PresetStart.make(prefixStr, ObsId(obsId)), "PresetStart", prefixStr),
        (PresetEnd.make(prefixStr, ObsId(obsId)), "PresetEnd", prefixStr),
        (GuidstarAcqStart.make(prefixStr, ObsId(obsId)), "GuidstarAcqStart", prefixStr),
        (GuidstarAcqEnd.make(prefixStr, ObsId(obsId)), "GuidstarAcqEnd", prefixStr),
        (ScitargetAcqStart.make(prefixStr, ObsId(obsId)), "ScitargetAcqStart", prefixStr),
        (ScitargetAcqEnd.make(prefixStr, ObsId(obsId)), "ScitargetAcqEnd", prefixStr),
        (ObservationStart.make(prefixStr, ObsId(obsId)), "ObservationStart", prefixStr),
        (ObservationEnd.make(prefixStr, ObsId(obsId)), "ObservationEnd", prefixStr),
        (ObserveStart.make(prefixStr, ObsId(obsId)), "ObserveStart", prefixStr),
        (ObserveEnd.make(prefixStr, ObsId(obsId)), "ObserveEnd", prefixStr)
      ).forEvery((observeEvent, expectedEventName, expectedPrefixStr) => {
        observeEvent.eventName should ===(EventName(expectedEventName))
        observeEvent.source should ===(Prefix(expectedPrefixStr))
        observeEvent.paramSet shouldBe Set(obsIdParam)
      })
    }

    "create Observe Event with obsId and exposure Id parameters | ESW-81" in {
      Table(
        ("Observe Event", "Event Name", "Prefix"),
        (ExposureStart.make(prefixStr, ObsId(obsId), exposureId), "ExposureStart", prefixStr),
        (ExposureEnd.make(prefixStr, ObsId(obsId), exposureId), "ExposureEnd", prefixStr),
        (readoutEnd.make(prefixStr, ObsId(obsId), exposureId), "readoutEnd", prefixStr),
        (readoutFailed.make(prefixStr, ObsId(obsId), exposureId), "readoutFailed", prefixStr),
        (dataWriteStart.make(prefixStr, ObsId(obsId), exposureId), "dataWriteStart", prefixStr),
        (dataWriteEnd.make(prefixStr, ObsId(obsId), exposureId), "dataWriteEnd", prefixStr),
        (PrepareStart.make(prefixStr, ObsId(obsId), exposureId), "PrepareStart", prefixStr)
      ).forEvery((observeEvent, expectedEventName, expectedPrefixStr) => {
        observeEvent.eventName should ===(EventName(expectedEventName))
        observeEvent.source should ===(Prefix(expectedPrefixStr))
        observeEvent.paramSet shouldBe Set(obsIdParam, exposureIdParam)
      })
    }

    "create Observe Event with fixed Parameter set | ESW-81" in {
      val event = DowntimeStart.make(prefixStr, ObsId(obsId), "bad weather")
      event.eventName should ===(EventName("DowntimeStart"))
      event.source should ===(Prefix(prefixStr))
      event.paramSet shouldBe Set(obsIdParam, StringKey.make("reason").set("bad weather"))
    }
  }
}
