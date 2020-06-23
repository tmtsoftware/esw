package esw.ocs.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.location.api.extensions.ActorExtension._
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import csw.params.commands.CommandResponse.Started
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.events.{Event, EventKey, SystemEvent}
import csw.prefix.models.Subsystem.{ESW, TCS}
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.FrameworkTestKit
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.models.ObsMode
import esw.ocs.app.wiring.SequencerWiring
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

import scala.concurrent.duration.DurationInt

class SequencerTestMultiJvmNode1 extends SequencerTest("http")
class SequencerTestMultiJvmNode2 extends SequencerTest("http")
class SequencerTestMultiJvmNode3 extends SequencerTest("http")

class SequencerTest(mode: String) extends LSNodeSpec(config = new TwoMembersAndSeed, mode) {

  import config._

  private val frameworkTestKit = FrameworkTestKit()
  import frameworkTestKit._

  private val ocsSubsystem        = ESW
  private val ocsSequencerObsMode = ObsMode("MoonNight")
  private val tcsSubsystem        = TCS
  private val tcsSequencerObsMode = ObsMode("moonnight")
  private val command1            = Setup(Prefix("esw.test"), CommandName("multi-node"), None)
  private val command2            = Setup(Prefix("esw.test"), CommandName("command-2"), None)
  private val sequence            = Sequence(command1, command2)
  private val sequenceComponentRef = AkkaLocation(
    AkkaConnection(ComponentId(Prefix(ESW, "primary"), SequenceComponent)),
    TestProbe[SequenceComponentMsg]().ref.toURI
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    runOn(seed) { locationTestKit.startLocationServer() }
    enterBarrier("Before all")
  }

  test("tcs sequencer should send sequence to downstream ocs sequencer which submits the command to sample assembly") {
    runOn(seed) {
      enterBarrier("event-server-started")
      val ocsSequencerWiring = new SequencerWiring(ocsSubsystem, ocsSequencerObsMode, sequenceComponentRef)
      ocsSequencerWiring.sequencerServer.start()

      enterBarrier("ocs-started")
      enterBarrier("tcs-started")
      enterBarrier("assembly-started")

      // creating subscriber for event which will be publish in onSubmit handler for sample assembly
      val testProbe                 = TestProbe[Event]()
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
      eventTestKit.start()
      enterBarrier("event-server-started")
      enterBarrier("ocs-started")

      val tcsSequencerWiring = new SequencerWiring(tcsSubsystem, tcsSequencerObsMode, sequenceComponentRef)
      tcsSequencerWiring.sequencerServer.start()
      enterBarrier("tcs-started")
      enterBarrier("assembly-started")

      val ocsSequencer = sequencerClient(ocsSubsystem, ocsSequencerObsMode)

      ocsSequencer.submit(sequence).await shouldBe a[Started]
      enterBarrier("submit-sequence-to-ocs")
    }

    runOn(member2) {
      enterBarrier("event-server-started")
      enterBarrier("ocs-started")
      enterBarrier("tcs-started")

      spawnStandalone(ConfigFactory.load("standalone.conf"))
      enterBarrier("assembly-started")

      enterBarrier("submit-sequence-to-ocs")
    }
    enterBarrier("end")
  }

  private def sequencerClient(subsystem: Subsystem, observingMode: ObsMode) = {
    val componentId = ComponentId(Prefix(subsystem, observingMode.name), ComponentType.Sequencer)
    val location    = locationService.resolve(HttpConnection(componentId), 5.seconds).futureValue.get
    SequencerApiFactory.make(location)
  }
}
