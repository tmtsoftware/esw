package esw.ocs.script

import csw.params.core.generics.KeyType.BooleanKey
import csw.params.events.{EventKey, EventName}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import csw.testkit.scaladsl.CSWService.EventServer
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequencerImpl
import esw.ocs.api.protocol.Ok
import esw.ocs.testkit.EswTestKit

class OnlineFlagTest extends EswTestKit(EventServer) {

  private val ocsSubsystem               = ESW
  private val ocsObservingMode           = "onlineFlag"
  private var ocsSequencer: SequencerApi = _

  override def beforeEach(): Unit = {
    val ocsSequencerRef = spawnSequencerRef(ocsSubsystem, ocsObservingMode)
    ocsSequencer = new SequencerImpl(ocsSequencerRef)
  }

  "isOnline flag should be toggled on/off depending on whether the sequencer is online/offline | ESW-287" in {
    val prefix        = Prefix("tcs.filter.wheel")
    val eventKey      = EventKey(prefix, EventName("online-flag"))
    val onlineFlagKey = BooleanKey.make("online-flag")

    // verify isOnline flag is set as true by default
    Thread.sleep(200)
    eventually {
      val receivedEvent = eventSubscriber.get(eventKey).futureValue
      receivedEvent.paramType.get(onlineFlagKey).get.values.head shouldBe true
    }

    // go-offline
    ocsSequencer.goOffline().futureValue should ===(Ok)

    // verify isOnline flag is set as false
    Thread.sleep(200)
    eventually {
      val receivedEvent2 = eventSubscriber.get(eventKey).futureValue
      receivedEvent2.paramType.get(onlineFlagKey).get.values.head shouldBe false
    }
  }
}
