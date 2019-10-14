package esw.ocs.impl.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Props, Scheduler, SpawnProtocol}
import akka.util.Timeout
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType, Location}
import csw.logging.client.scaladsl.LoggerFactory
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.protocol.{GetStatusResponse, LoadScriptError, LoadScriptResponse}
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.messages.SequenceComponentMsg.{GetStatus, LoadScript, Stop, UnloadScript}

import scala.concurrent.duration.DurationLong

class SequenceComponentBehaviorTest extends ScalaTestFrameworkTestKit with BaseTestSuite {
  private implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem
  val ocsSequenceComponentName                                         = "OCS_1"

  private val factory = new LoggerFactory("SequenceComponentTest")

  implicit val scheduler: Scheduler = typedSystem.scheduler
  implicit val timeOut: Timeout     = frameworkTestKit.timeout

  def sequencerWiring(
      packageId: String,
      observingMode: String,
      sequenceComponentName: Option[String]
  ): SequencerWiring =
    new SequencerWiring(packageId, observingMode, sequenceComponentName)

  private def createBehaviorTestKit(): BehaviorTestKit[SequenceComponentMsg] = BehaviorTestKit(
    Behaviors.setup[SequenceComponentMsg] { _ =>
      SequenceComponentBehavior.behavior(ocsSequenceComponentName, factory.getLogger, sequencerWiring(_, _, _).sequencerServer)
    }
  )

  "SequenceComponentBehavior" must {
    "load/unload script and get appropriate status | ESW-103" in {
      val sequenceComponentRef: ActorRef[SequenceComponentMsg] = (typedSystem ? { x: ActorRef[ActorRef[SequenceComponentMsg]] =>
        Spawn(
          SequenceComponentBehavior
            .behavior(ocsSequenceComponentName, factory.getLogger, sequencerWiring(_, _, _).sequencerServer),
          ocsSequenceComponentName,
          Props.empty,
          x
        )
      }).futureValue

      val loadScriptResponseProbe = TestProbe[LoadScriptResponse]
      val getStatusProbe          = TestProbe[GetStatusResponse]
      val packageId               = "esw"
      val observingMode           = "darknight"

      //LoadScript
      sequenceComponentRef ! LoadScript(packageId, observingMode, loadScriptResponseProbe.ref)

      //Assert if script loaded and returns AkkaLocation of sequencer
      val loadScriptLocationResponse: AkkaLocation = loadScriptResponseProbe.receiveMessage.response.rightValue
      loadScriptLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(s"$ocsSequenceComponentName@$packageId@$observingMode", ComponentType.Sequencer)
      )

      //GetStatus
      sequenceComponentRef ! GetStatus(getStatusProbe.ref)

      //Assert if get status returns AkkaLocation of sequencer currently running
      val getStatusLocationResponse: Location = getStatusProbe.receiveMessage(5.seconds).response.get
      getStatusLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(s"$ocsSequenceComponentName@$packageId@$observingMode", ComponentType.Sequencer)
      )

      //UnloadScript
      val unloadScriptProbe = TestProbe[Done]
      sequenceComponentRef ! UnloadScript(unloadScriptProbe.ref)

      unloadScriptProbe.expectMessageType[Done.type]

      //assert if GetStatus returns None after unloading sequencer script
      sequenceComponentRef ! GetStatus(getStatusProbe.ref)
      getStatusProbe.expectMessage(GetStatusResponse(None))
    }

    "load script and give LoadScriptError if sequencer is already running | ESW-103" in {
      val sequenceComponentRef: ActorRef[SequenceComponentMsg] = (typedSystem ? { x: ActorRef[ActorRef[SequenceComponentMsg]] =>
        Spawn(
          SequenceComponentBehavior
            .behavior(ocsSequenceComponentName, factory.getLogger, sequencerWiring(_, _, _).sequencerServer),
          ocsSequenceComponentName,
          Props.empty,
          x
        )
      }).futureValue

      val loadScriptResponseProbe = TestProbe[LoadScriptResponse]
      val packageId               = "iris"
      val observingMode           = "darknight"

      //LoadScript
      sequenceComponentRef ! LoadScript(packageId, observingMode, loadScriptResponseProbe.ref)

      //Assert if script loaded and returns AkkaLocation of sequencer
      val loadScriptLocationResponse: AkkaLocation = loadScriptResponseProbe.receiveMessage.response.rightValue
      loadScriptLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(s"$ocsSequenceComponentName@$packageId@$observingMode", ComponentType.Sequencer)
      )

      sequenceComponentRef ! LoadScript("tcs", "darknight", loadScriptResponseProbe.ref)
      loadScriptResponseProbe.receiveMessage.response.leftValue shouldBe LoadScriptError(
        "Loading script failed: Sequencer already running"
      )
    }

    "unload script and return Done if sequence component is not running any sequencer | ESW-103" in {
      val sequenceComponentRef: ActorRef[SequenceComponentMsg] = (typedSystem ? { x: ActorRef[ActorRef[SequenceComponentMsg]] =>
        Spawn(
          SequenceComponentBehavior
            .behavior(ocsSequenceComponentName, factory.getLogger, sequencerWiring(_, _, _).sequencerServer),
          ocsSequenceComponentName,
          Props.empty,
          x
        )
      }).futureValue

      val unloadScriptResponseProbe = TestProbe[Done]
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
