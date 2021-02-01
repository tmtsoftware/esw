package esw.ocs.dsl.params

import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.params.core.models.{ExposureId, ObsId}
import csw.params.events.EventName
import csw.prefix.models.Prefix
import esw.testcommons.BaseTestSuite
import org.scalatest.prop.TableDrivenPropertyChecks._

class SequencerObserveEventsTest extends BaseTestSuite {
  "SequencerObserveEvents" must {
    val obsIds = List("2020A-001-123", "2021A-011-153", "2038A-034-193")
    val exposureIds =
      List("2020A-001-123-TCS-DET-SCI0-0001", "2022A-001-123-TCS-DET-FFD1-0001", "2021A-001-123-TCS-DET-SCI2-1234")
    val obsId                         = randomFrom(obsIds)
    val exposureId                    = randomFrom(exposureIds)
    val prefix                        = Prefix(randomSubsystem, randomString(5))
    val obsIdParam: Parameter[_]      = StringKey.make("obsId").set(obsId)
    val exposureIdParam: Parameter[_] = StringKey.make("exposureId").set(exposureId)
    val sequencerObserveEvent         = new SequencerObserveEvent(prefix)

    "create Observe Event with obsId parameters | ESW-81" in {
      Table(
        ("Observe Event", "Event Name", "Prefix"),
        (sequencerObserveEvent.presetStart(ObsId(obsId)), "PresetStart", prefix),
        (sequencerObserveEvent.presetStart(ObsId(obsId)), "PresetStart", prefix),
        (sequencerObserveEvent.presetEnd(ObsId(obsId)), "PresetEnd", prefix),
        (sequencerObserveEvent.guidstarAcqStart(ObsId(obsId)), "GuidstarAcqStart", prefix),
        (sequencerObserveEvent.guidstarAcqEnd(ObsId(obsId)), "GuidstarAcqEnd", prefix),
        (sequencerObserveEvent.scitargetAcqStart(ObsId(obsId)), "ScitargetAcqStart", prefix),
        (sequencerObserveEvent.scitargetAcqEnd(ObsId(obsId)), "ScitargetAcqEnd", prefix),
        (sequencerObserveEvent.observationStart(ObsId(obsId)), "ObservationStart", prefix),
        (sequencerObserveEvent.observationEnd(ObsId(obsId)), "ObservationEnd", prefix),
        (sequencerObserveEvent.observeStart(ObsId(obsId)), "ObserveStart", prefix),
        (sequencerObserveEvent.observeEnd(ObsId(obsId)), "ObserveEnd", prefix)
      ).forEvery((observeEvent, expectedEventName, expectedPrefix) => {
        observeEvent.eventName should ===(EventName(expectedEventName))
        observeEvent.source should ===(expectedPrefix)
        observeEvent.paramSet shouldBe Set(obsIdParam)
      })
    }

    "create Observe Event with obsId and exposure Id parameters | ESW-81" in {
      Table(
        ("Observe Event", "Event Name", "Prefix"),
        (sequencerObserveEvent.exposureStart(ObsId(obsId), ExposureId(exposureId)), "ExposureStart", prefix),
        (sequencerObserveEvent.exposureEnd(ObsId(obsId), ExposureId(exposureId)), "ExposureEnd", prefix),
        (sequencerObserveEvent.readoutEnd(ObsId(obsId), ExposureId(exposureId)), "ReadoutEnd", prefix),
        (sequencerObserveEvent.readoutFailed(ObsId(obsId), ExposureId(exposureId)), "ReadoutFailed", prefix),
        (sequencerObserveEvent.dataWriteStart(ObsId(obsId), ExposureId(exposureId)), "DataWriteStart", prefix),
        (sequencerObserveEvent.dataWriteEnd(ObsId(obsId), ExposureId(exposureId)), "DataWriteEnd", prefix),
        (sequencerObserveEvent.prepareStart(ObsId(obsId), ExposureId(exposureId)), "PrepareStart", prefix)
      ).forEvery((observeEvent, expectedEventName, expectedPrefixStr) => {
        observeEvent.eventName should ===(EventName(expectedEventName))
        observeEvent.source should ===(expectedPrefixStr)
        observeEvent.paramSet shouldBe Set(obsIdParam, exposureIdParam)
      })
    }

    "create downtimeStart Observe Event with fixed Parameter set | ESW-81" in {
      val event = sequencerObserveEvent.downtimeStart(ObsId(obsId), "bad weather")
      event.eventName should ===(EventName("DowntimeStart"))
      event.source should ===(prefix)
      event.paramSet shouldBe Set(obsIdParam, StringKey.make("reason").set("bad weather"))
    }

    "create observePaused event | ESW-81" in {
      val event = sequencerObserveEvent.observePaused()
      event.eventName should ===(EventName("ObservePaused"))
      event.source should ===(prefix)
    }

    "create observeResumed event | ESW-81" in {
      val event = sequencerObserveEvent.observeResumed()
      event.eventName should ===(EventName("ObserveResumed"))
      event.source should ===(prefix)
    }
  }
}
