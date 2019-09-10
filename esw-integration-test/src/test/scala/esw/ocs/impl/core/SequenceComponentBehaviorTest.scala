package esw.ocs.impl.core

import akka.actor.Scheduler
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType, Location}
import csw.logging.client.scaladsl.LoggerFactory
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.responses.SequenceComponentResponse.{Done, GetStatusResponse, LoadScriptResponse}
import esw.ocs.api.responses.SequenceComponentResponse
import esw.ocs.api.responses.RegistrationError
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.messages.SequenceComponentMsg.{GetStatus, LoadScript, Stop, UnloadScript}

import scala.concurrent.duration.DurationLong

class SequenceComponentBehaviorTest extends ScalaTestFrameworkTestKit with BaseTestSuite {
  private implicit val typedSystem: ActorSystem[SpawnProtocol] = frameworkTestKit.actorSystem
  val ocsSequenceComponentName                                 = "OCS_1"

  private val factory = new LoggerFactory("SequenceComponentTest")

  implicit val scheduler: Scheduler = typedSystem.scheduler
  implicit val timeOut: Timeout     = frameworkTestKit.timeout

  def sequencerWiring(
      sequencerId: String,
      observingMode: String,
      sequenceComponentName: Option[String]
  ): SequencerWiring =
    new SequencerWiring(sequencerId, observingMode, sequenceComponentName)

  private def createBehaviorTestKit(): BehaviorTestKit[SequenceComponentMsg] = BehaviorTestKit(
    Behaviors.setup[SequenceComponentMsg] { _ =>
      SequenceComponentBehavior.behavior(ocsSequenceComponentName, factory.getLogger, sequencerWiring(_, _, _).sequencerServer)
    }
  )

  "SequenceComponentBehavior" must {
    "load/unload script and get appropriate status | ESW-103" in {
      val sequenceComponentRef: ActorRef[SequenceComponentMsg] = (typedSystem ? Spawn(
        SequenceComponentBehavior.behavior(ocsSequenceComponentName, factory.getLogger, sequencerWiring(_, _, _).sequencerServer),
        ocsSequenceComponentName
      )).futureValue

      val loadScriptResponseProbe = TestProbe[LoadScriptResponse]
      val getStatusProbe          = TestProbe[GetStatusResponse]
      val sequencerId             = "testSequencerId1"
      val observingMode           = "testObservingMode1"

      //LoadScript
      sequenceComponentRef ! LoadScript(sequencerId, observingMode, loadScriptResponseProbe.ref)

      //todo: try resolving from location service
      //Assert if script loaded and returns AkkaLocation of sequencer
      val loadScriptLocationResponse: AkkaLocation = loadScriptResponseProbe.receiveMessage.response.rightValue
      loadScriptLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(s"$ocsSequenceComponentName@$sequencerId@$observingMode", ComponentType.Sequencer)
      )

      //GetStatus
      sequenceComponentRef ! GetStatus(getStatusProbe.ref)

      //Assert if get status returns AkkaLocation of sequencer currently running
      val getStatusLocationResponse: Location = getStatusProbe.receiveMessage(5.seconds).response.get
      getStatusLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(s"$ocsSequenceComponentName@$sequencerId@$observingMode", ComponentType.Sequencer)
      )

      //UnloadScript
      val unloadScriptProbe = TestProbe[SequenceComponentResponse.Done.type]
      sequenceComponentRef ! UnloadScript(unloadScriptProbe.ref)

      unloadScriptProbe.expectMessageType[Done.type]

      //assert if GetStatus returns None after unloading sequencer script
      sequenceComponentRef ! GetStatus(getStatusProbe.ref)
      getStatusProbe.expectMessage(GetStatusResponse(None))
    }

    "load script and give LoadScriptError if sequencer is already running | ESW-103" in {
      val sequenceComponentRef: ActorRef[SequenceComponentMsg] = (typedSystem ? Spawn(
        SequenceComponentBehavior.behavior(ocsSequenceComponentName, factory.getLogger, sequencerWiring(_, _, _).sequencerServer),
        ocsSequenceComponentName
      )).futureValue

      val loadScriptResponseProbe = TestProbe[LoadScriptResponse]
      val sequencerId             = "testSequencerId2"
      val observingMode           = "testObservingMode2"

      //LoadScript
      sequenceComponentRef ! LoadScript(sequencerId, observingMode, loadScriptResponseProbe.ref)

      //Assert if script loaded and returns AkkaLocation of sequencer
      val loadScriptLocationResponse: AkkaLocation = loadScriptResponseProbe.receiveMessage.response.rightValue
      loadScriptLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(s"$ocsSequenceComponentName@$sequencerId@$observingMode", ComponentType.Sequencer)
      )

      sequenceComponentRef ! LoadScript("sequencerId3", "observingMode3", loadScriptResponseProbe.ref)
      loadScriptResponseProbe.receiveMessage.response.leftValue shouldBe RegistrationError(
        "Loading script failed: Sequencer already running"
      )
    }

    "unload script and return Done if sequence component is not running any sequencer | ESW-103" in {
      val sequenceComponentRef: ActorRef[SequenceComponentMsg] = (typedSystem ? Spawn(
        SequenceComponentBehavior.behavior(ocsSequenceComponentName, factory.getLogger, sequencerWiring(_, _, _).sequencerServer),
        ocsSequenceComponentName
      )).futureValue

      val unloadScriptResponseProbe = TestProbe[Done.type]
      val getStatusProbe            = TestProbe[GetStatusResponse]

      //assert if GetStatus returns None after unloading sequencer script
      sequenceComponentRef ! GetStatus(getStatusProbe.ref)
      getStatusProbe.expectMessage(GetStatusResponse(None))

      //UnloadScript
      sequenceComponentRef ! UnloadScript(unloadScriptResponseProbe.ref)

      //Assert if UnloadScript returns Done
      unloadScriptResponseProbe.expectMessage(Done)
    }

    "get killed if stop msg is received | ESW-103" in {
      val behaviorTestKit = createBehaviorTestKit()

      behaviorTestKit.run(Stop)

      behaviorTestKit.isAlive shouldEqual false
    }
  }
}
