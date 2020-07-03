package esw.ocs.script

import csw.params.core.generics.KeyType.{BooleanKey, IntKey}
import csw.params.events.{EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import csw.testkit.scaladsl.CSWService.EventServer
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.Ok
import esw.ocs.testkit.EswTestKit

class OnlineOfflineHandlerTest extends EswTestKit(EventServer) {

  private val ocsSubsystem               = ESW
  private val ocsObsMode                 = ObsMode("onlineFlag")
  private var ocsSequencer: SequencerApi = _

  override def beforeEach(): Unit = {
    ocsSequencer = spawnSequencerProxy(ocsSubsystem, ocsObsMode)
  }

  "onOnline/onOffline handler should be called again if goOnline/goOffline message received while sequencer is online/offline respectively | ESW-287" in {
    val prefix          = Prefix("tcs.filter.wheel")
    val onlineEventKey  = EventKey(prefix, EventName("go-online-handler"))
    val offlineEventKey = EventKey(prefix, EventName("go-offline-handler"))
    val eventKey        = EventKey(prefix, EventName("online-flag"))
    val onlineFlagKey   = BooleanKey.make("online-flag")
    val probe           = createTestProbe(Set(onlineEventKey, offlineEventKey))
    val onlineFlagProbe = createTestProbe(Set(eventKey))

    // verify isOnline flag is set as true by default
    val firstOnlineFlagEvent = onlineFlagProbe.expectMessageType[SystemEvent]
    firstOnlineFlagEvent.paramSet.head shouldBe onlineFlagKey.set(true)

    // go-online
    ocsSequencer.goOnline().futureValue should ===(Ok)
    val firstOnlineEvent = probe.expectMessageType[SystemEvent]

    // verify onGoOnline handler is called
    firstOnlineEvent.paramSet.head shouldBe IntKey.make("online-key").set(1)

    ocsSequencer.goOnline().futureValue should ===(Ok)
    val secondOnlineEvent = probe.expectMessageType[SystemEvent]
    // verify onGoOnline handler is called again
    secondOnlineEvent.paramSet.head shouldBe IntKey.make("online-key").set(2)

    // go-offline
    ocsSequencer.goOffline().futureValue should ===(Ok)
    val firstOfflineEvent = probe.expectMessageType[SystemEvent]

    // verify onGoOffline handler is called
    firstOfflineEvent.paramSet.head shouldBe IntKey.make("offline-key").set(1)

    ocsSequencer.goOffline().futureValue should ===(Ok)
    val secondOfflineEvent = probe.expectMessageType[SystemEvent]
    // verify onGoOffline handler is called again
    secondOfflineEvent.paramSet.head shouldBe IntKey.make("offline-key").set(2)

    // verify isOnline flag is set as false
    eventually {
      val secondOnlineFlagEvent = onlineFlagProbe.expectMessageType[SystemEvent]
      secondOnlineFlagEvent.paramSet.head shouldBe onlineFlagKey.set(false)
    }
  }
}
