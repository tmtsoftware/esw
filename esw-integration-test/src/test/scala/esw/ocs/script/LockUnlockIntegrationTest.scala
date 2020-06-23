package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.StringKey
import csw.params.events.EventKey
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import csw.testkit.scaladsl.CSWService.EventServer
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit

class LockUnlockIntegrationTest extends EswTestKit(EventServer) {
  private var ocsSequencer: SequencerApi = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    frameworkTestKit.spawnStandalone(ConfigFactory.load("standaloneAssembly.conf"))
  }

  override def beforeEach(): Unit = {
    ocsSequencer = spawnSequencerProxy(ESW, ObsMode("lockUnlockScript"))
  }

  override def afterEach(): Unit = shutdownAllSequencers()

  "Script" must {
    val lockingStringKey = StringKey.make("lockingResponse")
    val lockingEventKey  = EventKey("esw.ocs.lock_unlock.locking_response")

    "support locking components | ESW-126" in {
      val probe = TestProbe[String]()
      eventSubscriber
        .subscribeCallback(
          Set(lockingEventKey),
          event => {
            val param = event.paramType.get(lockingStringKey).flatMap(_.get(0))
            param.foreach(probe.ref ! _)
          }
        )

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
