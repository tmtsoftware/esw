package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import com.typesafe.config.ConfigFactory
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.SubmitSequenceAndWait
import csw.event.client.EventServiceFactory
import csw.location.api.extensions.URIExtension.RichURI
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import csw.params.events.EventKey
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.app.wiring.SequencerWiring
import org.scalatest.time.SpanSugar.convertDoubleToGrainOfTime

class LockUnlockIntegrationTest extends ScalaTestFrameworkTestKit(EventServer) with BaseTestSuite {
  import frameworkTestKit.frameworkWiring.locationService
  import frameworkTestKit.frameworkWiring.actorRuntime._

  private var ocsWiring: SequencerWiring           = _
  private var ocsSequencer: ActorRef[SequencerMsg] = _

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  override def beforeAll(): Unit = {
    super.beforeAll()
    frameworkTestKit.spawnStandalone(ConfigFactory.load("standalone.conf"))
  }

  override def beforeEach(): Unit = {
    ocsWiring = new SequencerWiring("esw", "lockUnlockScript", None)
    ocsSequencer = ocsWiring.sequencerServer.start().rightValue.uri.toActorRef.unsafeUpcast[SequencerMsg]
  }

  override def afterEach(): Unit = {
    ocsWiring.sequencerServer.shutDown().futureValue
  }

  "Script" must {
    "support locking/unlocking components | ESW-126" in {
      val lockCommand     = Setup(Prefix("TCS.test"), CommandName("lock-assembly"), None)
      val unlockCommand   = Setup(Prefix("TCS.test"), CommandName("unlock-assembly"), None)
      val sequence        = Sequence(lockCommand, unlockCommand)
      val eventSubscriber = new EventServiceFactory().make(locationService).defaultSubscriber

      ocsSequencer ! SubmitSequenceAndWait(sequence, TestProbe[SubmitResponse].ref)

      eventually {
        val lockResponse = eventSubscriber.get(EventKey("csw.assembly.lock_response")).futureValue
        lockResponse.eventId.id shouldNot be("-1")
      }

      eventually {
        val unlockResponse = eventSubscriber.get(EventKey("csw.assembly.unlock_response")).futureValue
        unlockResponse.eventId.id shouldNot be("-1")
      }
    }
  }
}
