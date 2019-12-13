package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import com.typesafe.config.ConfigFactory
import csw.command.client.messages.sequencer.SequencerMsg
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.StringKey
import csw.params.events.EventKey
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.SequencerApi
import esw.ocs.impl.SequencerActorProxy
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.EventServer

class LockUnlockIntegrationTest extends EswTestKit(EventServer) {
  private var ocsSequencerRef: ActorRef[SequencerMsg] = _
  private var ocsSequencer: SequencerApi              = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    frameworkTestKit.spawnStandalone(ConfigFactory.load("standalone.conf"))
  }

  override def beforeEach(): Unit = {
    ocsSequencerRef = spawnSequencerRef(ESW, "lockUnlockScript")
    ocsSequencer = new SequencerActorProxy(ocsSequencerRef)
  }

  override def afterEach(): Unit = shutdownAllSequencers()

  "Script" must {
    val lockingStringKey = StringKey.make("lockingResponse")
    val lockingEventKey  = EventKey("esw.ocs.lock_unlock.locking_response")

    "support locking components | ESW-126" in {
      val probe = TestProbe[String]
      eventSubscriber
        .subscribeCallback(Set(lockingEventKey), event => {
          val param = event.paramType.get(lockingStringKey).flatMap(_.get(0))
          param.foreach(probe.ref ! _)
        })

      val lockCommand = Setup(Prefix("TCS.test"), CommandName("lock-assembly"), None)
      ocsSequencer.submitAndWait(Sequence(lockCommand))

      probe.expectMessage("LockAcquired$")
      probe.expectMessage("LockExpiringShortly$")
      probe.expectMessage("LockExpired$")
    }

    "support unlocking components | ESW-126" in {
      val unlockCommand = Setup(Prefix("TCS.test"), CommandName("unlock-assembly"), None)
      ocsSequencer.submitAndWait(Sequence(unlockCommand))
      eventually {
        val unlockEvent = eventSubscriber.get(lockingEventKey).futureValue
        unlockEvent.paramType.get(lockingStringKey).flatMap(_.get(0)) should ===(Some("LockAlreadyReleased$"))
      }
    }
  }
}
