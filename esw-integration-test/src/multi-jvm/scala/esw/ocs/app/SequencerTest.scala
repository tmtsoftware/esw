package esw.ocs.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import csw.location.models.Connection.HttpConnection
import csw.location.models.{ComponentId, ComponentType}
import csw.location.server.http.MultiNodeHTTPLocationService
import csw.params.commands.CommandResponse.Started
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import csw.params.events.{Event, EventKey, SystemEvent}
import csw.testkit.{EventTestKit, FrameworkTestKit}
import esw.ocs.api.SequencerApi
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.SequencerApiFactory
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

import scala.concurrent.duration.DurationInt

class SequencerTestMultiJvmNode1 extends SequencerTest(0, "http")
class SequencerTestMultiJvmNode2 extends SequencerTest(0, "http")
class SequencerTestMultiJvmNode3 extends SequencerTest(0, "http")

class SequencerTest(ignore: Int, mode: String)
    extends LSNodeSpec(config = new TwoMembersAndSeed, mode)
    with MultiNodeHTTPLocationService {

  import config._

  private val frameworkTestKit = FrameworkTestKit()
  private val eventTestKit     = EventTestKit()

  private val ocsSequencerId      = "ocs"
  private val ocsSequencerObsMode = "moonnight"
  private val tcsSequencerId      = "tcs"
  private val tcsSequencerObsMode = "moonnight"
  private val command1            = Setup(Prefix("esw.test"), CommandName("multi-node"), None)
  private val command2            = Setup(Prefix("esw.test"), CommandName("command-2"), None)
  private val sequence            = Sequence(command1, command2)

  test("tcs sequencer should send sequence to downstream ocs sequencer which submits the command to sample assembly") {
    runOn(seed) {
      val ocsSequencerWiring = new SequencerWiring(ocsSequencerId, ocsSequencerObsMode, None)
      ocsSequencerWiring.sequencerServer.start()

      enterBarrier("ocs-started")
      enterBarrier("tcs-started")
      enterBarrier("assembly-started")

      enterBarrier("event-server-started")
      // creating subscriber for event which will be publish in onSubmit handler for sample assembly
      val testProbe                 = TestProbe[Event]
      val eventSubscriber           = ocsSequencerWiring.cswWiring.eventService.defaultSubscriber
      val multiJVMCommandEventKey   = EventKey("tcs.filter.wheel.setup-command-from-tcs-sequencer")
      val multiJVMEventSubscription = eventSubscriber.subscribeActorRef(Set(multiJVMCommandEventKey), testProbe.ref)
      multiJVMEventSubscription.ready().await
      testProbe.expectMessageType[SystemEvent] // discard invalid event
      //##############
      enterBarrier("submit-sequence-to-ocs")
      Thread.sleep(500)

      val multiJVMCommandEvent = testProbe.expectMessageType[SystemEvent]
      multiJVMCommandEvent.isInvalid should ===(false)
    }

    runOn(member1) {
      enterBarrier("ocs-started")

      val tcsSequencerWiring = new SequencerWiring(tcsSequencerId, tcsSequencerObsMode, None)
      tcsSequencerWiring.sequencerServer.start()
      enterBarrier("tcs-started")
      enterBarrier("assembly-started")

      eventTestKit.start()
      enterBarrier("event-server-started")

      val ocsSequencer = sequencerClient(ocsSequencerId, ocsSequencerObsMode)

      ocsSequencer.submit(sequence).await shouldBe a[Started]
      enterBarrier("submit-sequence-to-ocs")
    }

    runOn(member2) {
      enterBarrier("ocs-started")
      enterBarrier("tcs-started")

      frameworkTestKit.spawnStandalone(ConfigFactory.load("standalone.conf"))
      enterBarrier("assembly-started")

      enterBarrier("event-server-started")
      enterBarrier("submit-sequence-to-ocs")
    }
    enterBarrier("end")
  }

  private def sequencerClient(packageId: String, observingMode: String): SequencerApi = {
    val componentId = ComponentId(s"$packageId@$observingMode", ComponentType.Sequencer)
    val location    = locationService.resolve(HttpConnection(componentId), 5.seconds).futureValue.get
    SequencerApiFactory.make(location)
  }
}
