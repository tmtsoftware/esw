package esw.ocs.impl.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Props}
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType, Location}
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.core.models.Prefix
import esw.ocs.api.protocol.{GetStatusResponse, ScriptError, ScriptResponse}
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.messages.SequenceComponentMsg._
import esw.ocs.testkit.EswTestKit

import scala.concurrent.duration.DurationLong

class SequenceComponentBehaviorTest extends EswTestKit {
  private val ocsSequenceComponentName = "ESW.ESW_1"
  private val factory                  = new LoggerFactory("SequenceComponentTest")

  private def spawnSequenceComponent() = {
    (system ? { x: ActorRef[ActorRef[SequenceComponentMsg]] =>
      Spawn(
        Behaviors.setup[SequenceComponentMsg] { ctx =>
          SequenceComponentBehavior.behavior(ctx.self, factory.getLogger, sequencerWiring(_, _, _).sequencerServer)
        },
        ocsSequenceComponentName,
        Props.empty,
        x
      )
    }).futureValue
  }

  def sequencerWiring(
      packageId: String,
      observingMode: String,
      sequenceComponent: ActorRef[SequenceComponentMsg]
  ): SequencerWiring =
    new SequencerWiring(packageId, observingMode, sequenceComponent)

  private def createBehaviorTestKit(): BehaviorTestKit[SequenceComponentMsg] = BehaviorTestKit(
    Behaviors.setup[SequenceComponentMsg] { ctx =>
      SequenceComponentBehavior.behavior(ctx.self, factory.getLogger, sequencerWiring(_, _, _).sequencerServer)
    }
  )

  "SequenceComponentBehavior" must {
    "load/unload script and get appropriate status | ESW-103" in {
      val sequenceComponentRef: ActorRef[SequenceComponentMsg] = spawnSequenceComponent()

      val loadScriptResponseProbe = TestProbe[ScriptResponse]
      val getStatusProbe          = TestProbe[GetStatusResponse]
      val packageId               = "esw"
      val observingMode           = "darknight"
      val prefix                  = Prefix(s"$packageId.$observingMode")

      //LoadScript
      sequenceComponentRef ! LoadScript(packageId, observingMode, loadScriptResponseProbe.ref)

      //Assert if script loaded and returns AkkaLocation of sequencer
      val loadScriptLocationResponse: AkkaLocation = loadScriptResponseProbe.receiveMessage.response.rightValue
      loadScriptLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(prefix, ComponentType.Sequencer)
      )

      //GetStatus
      sequenceComponentRef ! GetStatus(getStatusProbe.ref)

      //Assert if get status returns AkkaLocation of sequencer currently running
      val getStatusLocationResponse: Location = getStatusProbe.receiveMessage(5.seconds).response.get
      getStatusLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(prefix, ComponentType.Sequencer)
      )

      //UnloadScript
      val unloadScriptProbe = TestProbe[Done]
      sequenceComponentRef ! UnloadScript(unloadScriptProbe.ref)

      unloadScriptProbe.expectMessageType[Done.type]

      //assert if GetStatus returns None after unloading sequencer script
      sequenceComponentRef ! GetStatus(getStatusProbe.ref)
      getStatusProbe.expectMessage(GetStatusResponse(None))
    }

    "load script and give ScriptError if sequencer is already running | ESW-103" in {
      val sequenceComponentRef: ActorRef[SequenceComponentMsg] = spawnSequenceComponent()

      val loadScriptResponseProbe = TestProbe[ScriptResponse]
      val packageId               = "iris"
      val observingMode           = "darknight"
      val prefix                  = Prefix(s"$packageId.$observingMode")

      //LoadScript
      sequenceComponentRef ! LoadScript(packageId, observingMode, loadScriptResponseProbe.ref)

      //Assert if script loaded and returns AkkaLocation of sequencer
      val loadScriptLocationResponse: AkkaLocation = loadScriptResponseProbe.receiveMessage.response.rightValue
      loadScriptLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(prefix, ComponentType.Sequencer)
      )

      sequenceComponentRef ! LoadScript("tcs", "darknight", loadScriptResponseProbe.ref)
      loadScriptResponseProbe.receiveMessage.response.leftValue shouldBe ScriptError(
        "Loading script failed: Sequencer already running"
      )
    }

    "unload script and return Done if sequence component is not running any sequencer | ESW-103" in {
      val sequenceComponentRef: ActorRef[SequenceComponentMsg] = spawnSequenceComponent()

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

    "restart sequencer if sequence component is in running state | ESW-141" in {
      val sequenceComponentRef: ActorRef[SequenceComponentMsg] = spawnSequenceComponent()

      val packageId               = "esw"
      val observingMode           = "darknight"
      val prefix                  = Prefix(s"$packageId.$observingMode")
      val loadScriptResponseProbe = TestProbe[ScriptResponse]
      val restartResponseProbe    = TestProbe[ScriptResponse]

      //Assert if script loaded and returns AkkaLocation of sequencer
      sequenceComponentRef ! LoadScript(packageId, observingMode, loadScriptResponseProbe.ref)
      loadScriptResponseProbe.expectMessageType[ScriptResponse]

      //Restart sequencer and assert if it returns new AkkaLocation of sequencer
      sequenceComponentRef ! Restart(restartResponseProbe.ref)

      val restartLocationResponse: AkkaLocation = restartResponseProbe.receiveMessage.response.rightValue
      restartLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(prefix, ComponentType.Sequencer)
      )
    }

    "restart should fail if sequencer is in idle state | ESW-141" in {
      val sequenceComponentRef: ActorRef[SequenceComponentMsg] = spawnSequenceComponent()

      val restartResponseProbe = TestProbe[ScriptResponse]
      sequenceComponentRef ! Restart(restartResponseProbe.ref)
      restartResponseProbe.expectMessage(ScriptResponse(Left(ScriptError("Restart is not supported in idle state"))))
    }

    "get killed if stop msg is received | ESW-103" in {
      val behaviorTestKit = createBehaviorTestKit()

      behaviorTestKit.run(Stop)

      behaviorTestKit.isAlive shouldEqual false
    }
  }
}
