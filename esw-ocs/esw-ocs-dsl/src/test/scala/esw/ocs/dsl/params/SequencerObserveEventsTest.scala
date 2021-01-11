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
        (presetStart(prefixStr, ObsId(obsId)), "PresetStart", prefixStr),
        (presetStart(prefixStr, ObsId(obsId)), "PresetStart", prefixStr),
        (presetEnd(prefixStr, ObsId(obsId)), "PresetEnd", prefixStr),
        (guidstarAcqStart(prefixStr, ObsId(obsId)), "GuidstarAcqStart", prefixStr),
        (guidstarAcqEnd(prefixStr, ObsId(obsId)), "GuidstarAcqEnd", prefixStr),
        (scitargetAcqStart(prefixStr, ObsId(obsId)), "ScitargetAcqStart", prefixStr),
        (scitargetAcqEnd(prefixStr, ObsId(obsId)), "ScitargetAcqEnd", prefixStr),
        (observationStart(prefixStr, ObsId(obsId)), "ObservationStart", prefixStr),
        (observationEnd(prefixStr, ObsId(obsId)), "ObservationEnd", prefixStr),
        (observeStart(prefixStr, ObsId(obsId)), "ObserveStart", prefixStr),
        (observeEnd(prefixStr, ObsId(obsId)), "ObserveEnd", prefixStr)
      ).forEvery((observeEvent, expectedEventName, expectedPrefixStr) => {
        observeEvent.eventName should ===(EventName(expectedEventName))
        observeEvent.source should ===(Prefix(expectedPrefixStr))
        observeEvent.paramSet shouldBe Set(obsIdParam)
      })
    }

    "create Observe Event with obsId and exposure Id parameters | ESW-81" in {
      Table(
        ("Observe Event", "Event Name", "Prefix"),
        (exposureStart(prefixStr, ObsId(obsId), exposureId), "ExposureStart", prefixStr),
        (exposureEnd(prefixStr, ObsId(obsId), exposureId), "ExposureEnd", prefixStr),
        (readoutEnd(prefixStr, ObsId(obsId), exposureId), "ReadoutEnd", prefixStr),
        (readoutFailed(prefixStr, ObsId(obsId), exposureId), "ReadoutFailed", prefixStr),
        (dataWriteStart(prefixStr, ObsId(obsId), exposureId), "DataWriteStart", prefixStr),
        (dataWriteEnd(prefixStr, ObsId(obsId), exposureId), "DataWriteEnd", prefixStr),
        (prepareStart(prefixStr, ObsId(obsId), exposureId), "PrepareStart", prefixStr)
      ).forEvery((observeEvent, expectedEventName, expectedPrefixStr) => {
        observeEvent.eventName should ===(EventName(expectedEventName))
        observeEvent.source should ===(Prefix(expectedPrefixStr))
        observeEvent.paramSet shouldBe Set(obsIdParam, exposureIdParam)
      })
    }

    "create downtimeStart Observe Event with fixed Parameter set | ESW-81" in {
      val event = downtimeStart(prefixStr, ObsId(obsId), "bad weather")
      event.eventName should ===(EventName("DowntimeStart"))
      event.source should ===(Prefix(prefixStr))
      event.paramSet shouldBe Set(obsIdParam, StringKey.make("reason").set("bad weather"))
    }

    "create observePaused event | ESW-81" in {
      val event = observePaused(prefixStr)
      event.eventName should ===(EventName("ObservePaused"))
      event.source should ===(Prefix(prefixStr))
    }

    "create observeResumed event | ESW-81" in {
      val event = observeResumed(prefixStr)
      event.eventName should ===(EventName("ObserveResumed"))
      event.source should ===(Prefix(prefixStr))
    }
  }
}
