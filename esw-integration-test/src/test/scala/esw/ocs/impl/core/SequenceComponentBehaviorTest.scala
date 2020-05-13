package esw.ocs.impl.core

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Props}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType, Location}
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg._
import esw.ocs.api.protocol.{GetStatusResponse, LoadScriptError, LoadScriptResponse}
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.testkit.EswTestKit

import scala.concurrent.duration.DurationLong

class SequenceComponentBehaviorTest extends EswTestKit {
  private val ocsSequenceComponentName = "ESW.ESW_1"
  private val factory                  = new LoggerFactory(Prefix("csw.SequenceComponentTest"))

  private def spawnSequenceComponent() = {
    (actorSystem ? { replyTo: ActorRef[ActorRef[SequenceComponentMsg]] =>
      Spawn(
        SequenceComponentBehavior
          .behavior(Prefix(ocsSequenceComponentName), factory.getLogger, new SequencerWiring(_, _, _).sequencerServer),
        ocsSequenceComponentName,
        Props.empty,
        replyTo
      )
    }).futureValue
  }

  "SequenceComponentBehavior" must {
    "load/unload script and get appropriate status | ESW-103, ESW-255" in {
      val sequenceComponentRef: ActorRef[SequenceComponentMsg] = spawnSequenceComponent()

      val loadScriptResponseProbe = TestProbe[LoadScriptResponse]
      val getStatusProbe          = TestProbe[GetStatusResponse]
      val subsystem               = ESW
      val observingMode           = "darknight"
      val prefix                  = Prefix(s"$subsystem.$observingMode")

      //LoadScript
      sequenceComponentRef ! LoadScript(subsystem, observingMode, loadScriptResponseProbe.ref)

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

      val loadScriptResponseProbe = TestProbe[LoadScriptResponse]
      val subsystem               = IRIS
      val observingMode           = "darknight"
      val prefix                  = Prefix(s"$subsystem.$observingMode")

      //LoadScript
      sequenceComponentRef ! LoadScript(subsystem, observingMode, loadScriptResponseProbe.ref)

      //Assert if script loaded and returns AkkaLocation of sequencer
      val loadScriptLocationResponse: AkkaLocation = loadScriptResponseProbe.receiveMessage.response.rightValue
      loadScriptLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(prefix, ComponentType.Sequencer)
      )

      sequenceComponentRef ! LoadScript(TCS, "darknight", loadScriptResponseProbe.ref)
      loadScriptResponseProbe.receiveMessage.response.leftValue shouldBe LoadScriptError(
        "Loading script failed: Sequencer already running"
      )
    }

    "load script and give ScriptError if exception on initialization | ESW-243" in {
      val sequenceComponentRef: ActorRef[SequenceComponentMsg] = spawnSequenceComponent()

      val loadScriptResponseProbe = TestProbe[LoadScriptResponse]
      val subsystem               = ESW
      val observingMode           = "initException"

      //LoadScript
      sequenceComponentRef ! LoadScript(subsystem, observingMode, loadScriptResponseProbe.ref)

      loadScriptResponseProbe.receiveMessage.response.leftValue shouldBe LoadScriptError(
        "Script initialization failed with : initialisation failed"
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

    "restart sequencer if sequence component is in running state (sequencer can be in any state) | ESW-141" in {
      val sequenceComponentRef: ActorRef[SequenceComponentMsg] = spawnSequenceComponent()

      val subsystem               = ESW
      val observingMode           = "darknight"
      val prefix                  = Prefix(s"$subsystem.$observingMode")
      val loadScriptResponseProbe = TestProbe[LoadScriptResponse]
      val restartResponseProbe    = TestProbe[LoadScriptResponse]

      //Assert if script loaded and returns AkkaLocation of sequencer
      sequenceComponentRef ! LoadScript(subsystem, observingMode, loadScriptResponseProbe.ref)
      val message = loadScriptResponseProbe.receiveMessage
      message shouldBe a[LoadScriptResponse]
      message.response.isRight shouldBe true
      val initialLocation = message.response.rightValue

      //Restart sequencer and assert if it returns new AkkaLocation of sequencer
      sequenceComponentRef ! Restart(restartResponseProbe.ref)

      val restartLocationResponse: AkkaLocation = restartResponseProbe.receiveMessage.response.rightValue
      restartLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(prefix, ComponentType.Sequencer)
      )
      restartLocationResponse should not equal initialLocation
    }

    "restart should fail if sequence component is in idle state | ESW-141" in {
      val sequenceComponentRef: ActorRef[SequenceComponentMsg] = spawnSequenceComponent()

      val restartResponseProbe = TestProbe[LoadScriptResponse]
      sequenceComponentRef ! Restart(restartResponseProbe.ref)
      restartResponseProbe.expectMessage(LoadScriptResponse(Left(LoadScriptError("Restart is not supported in idle state"))))
    }
  }
}
