package esw.ocs.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import com.typesafe.config.ConfigFactory
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.params.commands.CommandResponse.Started
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.events.{Event, EventKey, SystemEvent}
import csw.prefix.models.Subsystem.{ESW, TCS}
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.FrameworkTestKit
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.models.ObsMode
import esw.ocs.app.wiring.SequencerWiring
import esw.{MultiNodeSampleConfig, STMultiNodeSpec}

import scala.concurrent.duration.DurationInt

class SequencerTestMultiJvmNode1 extends SequencerTest
class SequencerTestMultiJvmNode2 extends SequencerTest
class SequencerTestMultiJvmNode3 extends SequencerTest

class SequencerTest extends MultiNodeSpec(MultiNodeSampleConfig) with STMultiNodeSpec with ImplicitSender {

  import MultiNodeSampleConfig._

  private val frameworkTestKit = FrameworkTestKit()
  import frameworkTestKit._
  private implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = frameworkWiring.actorSystem

  private lazy val locationService = frameworkWiring.locationService

  private val ocsSubsystem                    = ESW
  private val ocsSequencerObsMode             = ObsMode("MoonNight")
  private val tcsSubsystem                    = TCS
  private val tcsSequencerObsMode             = ObsMode("moonnight")
  private val command1                        = Setup(Prefix("esw.test"), CommandName("multi-node"), None)
  private val command2                        = Setup(Prefix("esw.test"), CommandName("command-2"), None)
  private val sequence                        = Sequence(command1, command2)
  private val sequenceComponentPrefix: Prefix = Prefix(ESW, "primary")

  override def initialParticipants: Int = roles.size
  LoggingSystemFactory.forTestingOnly()

  override def beforeAll(): Unit = {
    super.beforeAll()
    runOn(node1) { locationTestKit.startLocationServer() }
    enterBarrier("Before all")
  }

  test("tcs sequencer should send sequence to downstream ocs sequencer which submits the command to sample assembly") {
    runOn(node1) {
      enterBarrier("event-server-started")
      val ocsSequencerWiring = new SequencerWiring(ocsSubsystem, ocsSequencerObsMode, sequenceComponentPrefix)
      ocsSequencerWiring.sequencerServer.start()

      enterBarrier("ocs-started")
      enterBarrier("tcs-started")
      enterBarrier("assembly-started")

      // creating subscriber for event which will be publish in onSubmit handler for sample assembly
      val testProbe                 = TestProbe[Event]()
      val eventSubscriber           = ocsSequencerWiring.eventService.defaultSubscriber
      val multiJVMCommandEventKey   = EventKey("tcs.filter.wheel.setup-command-from-tcs-sequencer")
      val multiJVMEventSubscription = eventSubscriber.subscribeActorRef(Set(multiJVMCommandEventKey), testProbe.ref)
      multiJVMEventSubscription.ready().await
      testProbe.expectMessageType[SystemEvent] // discard invalid event
      //##############
      enterBarrier("submit-sequence-to-ocs")
      Thread.sleep(500)

      val multiJVMCommandEvent = testProbe.expectMessageType[SystemEvent](10.seconds)
      multiJVMCommandEvent.isInvalid should ===(false)
    }

    runOn(node2) {
      eventTestKit.start()
      enterBarrier("event-server-started")
      enterBarrier("ocs-started")

      val tcsSequencerWiring = new SequencerWiring(tcsSubsystem, tcsSequencerObsMode, sequenceComponentPrefix)
      tcsSequencerWiring.sequencerServer.start()
      enterBarrier("tcs-started")
      enterBarrier("assembly-started")

      val ocsSequencer = sequencerClient(ocsSubsystem, ocsSequencerObsMode)

      ocsSequencer.submit(sequence).await shouldBe a[Started]
      enterBarrier("submit-sequence-to-ocs")
    }

    runOn(node3) {
      enterBarrier("event-server-started")
      enterBarrier("ocs-started")
      enterBarrier("tcs-started")

      spawnStandalone(ConfigFactory.load("standalone.conf"))
      enterBarrier("assembly-started")

      enterBarrier("submit-sequence-to-ocs")
    }
    enterBarrier("end")
  }

  private def sequencerClient(subsystem: Subsystem, obsMode: ObsMode) = {
    val componentId = ComponentId(Prefix(subsystem, obsMode.name), ComponentType.Sequencer)
    val location    = locationService.resolve(HttpConnection(componentId), 5.seconds).futureValue.get
    SequencerApiFactory.make(location)
  }
}
